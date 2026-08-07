package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginRequest;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.RefreshRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.entity.LoginLog;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.repository.LoginLogRepository;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import com.ticketingsystem.yuzhonblog.util.CaptchaUtil;
import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionStore sessionStore;
    private final CaptchaUtil captchaUtil;
    private final PermissionService permissionService;
    private final IpExtractor ipExtractor;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AdminUser user = adminUserRepository.findByUsername(request.getUsername()).orElse(null);

        // User not found — use generic message to prevent username enumeration
        if (user == null) {
            saveLoginLog(null, request.getUsername(), ip, userAgent, 0, "用户不存在");
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // Account disabled — same error code as login failed to prevent enumeration
        // Super admin auto-unfreeze: if super_admin is disabled, re-enable and allow login
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            if ("super_admin".equals(user.getRole())) {
                user.setEnabled(true);
                adminUserRepository.save(user);
                log.info("Super admin auto-unfrozen: {}", user.getUsername());
            } else {
                saveLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "账号已禁用");
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
        }

        // Account locked — same error code as login failed to prevent enumeration
        if (user.getLockUntil() != null && LocalDateTime.now().isBefore(user.getLockUntil())) {
            saveLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "账号已锁定");
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // Auto-unlock if lock period expired (including exact expiry time)
        if (user.getLockUntil() != null && !LocalDateTime.now().isBefore(user.getLockUntil())) {
            user.setLockUntil(null);
            user.setFailedAttempts(0);
            user.setLockCount(0);
            adminUserRepository.save(user);
        }

        // Captcha required after 2 failed attempts
        if (user.getFailedAttempts() != null && user.getFailedAttempts() >= 2) {
            if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
                throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
            }
            if (!captchaUtil.verify(request.getCaptchaId(), request.getCaptchaCode())) {
                saveLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "验证码错误");
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
        }

        // Password check
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(user, ip, userAgent);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // Success: reset failed attempts and lock count
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setLockCount(0);
        adminUserRepository.save(user);

        // Generate tokens with permissions
        var permissions = permissionService.getEffectivePermissions(user.getId(), user.getRole());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getId(), permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Store session (single-device enforcement, track both tokens)
        sessionStore.storeToken(user.getId(), accessToken, refreshToken, userAgent, user.getRole(), ip, "密码登录", true);

        // Log success
        saveLoginLog(user.getId(), user.getUsername(), ip, userAgent, 1, null);

        log.info("Login success: username={}, ip={}, device={}", user.getUsername(), ip,
                SessionStore.parseDeviceInfo(userAgent));

        return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getName(), user.getAvatarUrl(), user.getRole(), permissions);
    }

    @Transactional
    public LoginResponse refreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        String refreshTokenStr = request.getRefreshToken();

        if (!jwtUtil.validateTokenType(refreshTokenStr, "refresh")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        String username = jwtUtil.getUsernameFromToken(refreshTokenStr);
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Atomically consume refresh token (one-time-use, prevents concurrent reuse)
        if (!sessionStore.consumeRefreshToken(user.getId(), refreshTokenStr)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        var permissions = permissionService.getEffectivePermissions(user.getId(), user.getRole());
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getId(), permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Update session with new tokens
        String userAgent = httpRequest.getHeader("User-Agent");
        sessionStore.storeToken(user.getId(), newAccessToken, newRefreshToken, userAgent, user.getRole());

        return new LoginResponse(newAccessToken, newRefreshToken, user.getUsername(), user.getName(), user.getAvatarUrl(), user.getRole(), permissions);
    }

    public void logout(Long userId) {
        sessionStore.removeToken(userId);
    }

    private void recordLoginFailure(AdminUser user, String ip, String userAgent) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            int lockCount = (user.getLockCount() == null ? 0 : user.getLockCount()) + 1;
            user.setLockCount(lockCount);
            // Progressive lockout: 5min, 10min, 15min+
            int lockMinutes = switch (lockCount) {
                case 1 -> 5;
                case 2 -> 10;
                default -> 15;
            };
            user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            log.warn("Account locked: username={}, lockMinutes={}, lockCount={}",
                    user.getUsername(), lockMinutes, lockCount);
        }

        adminUserRepository.save(user);
        saveLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0,
                "密码错误 (" + attempts + "/" + MAX_FAILED_ATTEMPTS + ")");
    }

    private void saveLoginLog(Long userId, String username, String ip, String userAgent,
                              int status, String failReason) {
        try {
            LoginLog logEntry = new LoginLog();
            logEntry.setUserId(userId);
            logEntry.setUsername(username);
            logEntry.setLoginIp(ip);
            logEntry.setUserAgent(userAgent);
            logEntry.setDeviceInfo(SessionStore.parseDeviceInfo(userAgent));
            logEntry.setStatus(status);
            logEntry.setFailReason(failReason);
            loginLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save login log", e);
        }
    }

    private String extractIp(HttpServletRequest request) {
        return ipExtractor.extractClientIp(request);
    }
}
