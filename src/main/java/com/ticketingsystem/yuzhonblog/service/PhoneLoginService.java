package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginResponse;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.entity.PhoneBinding;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.repository.PhoneBindingRepository;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneLoginService {

    private final PhoneBindingRepository phoneBindingRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionStore sessionStore;
    private final PermissionService permissionService;
    private final IpExtractor ipExtractor;

    private static final String CODE_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.phone.simulation:false}")
    private boolean simulation;

    @Transactional
    public void bindPhone(Long userId, String phone, String unlockPassword) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_INVALID_FORMAT);
        }
        if (phoneBindingRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
        }

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<PhoneBinding> existing = phoneBindingRepository.findByUserId(userId);
        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
        }

        PhoneBinding binding = new PhoneBinding();
        binding.setUser(user);
        binding.setPhone(phone);
        binding.setUnlockPassword(passwordEncoder.encode(unlockPassword));
        phoneBindingRepository.save(binding);

        user.setPhone(phone);
        adminUserRepository.save(user);

        log.info("Phone bound: userId={}, phone={}", userId, maskPhone(phone));
    }

    @Transactional
    public void changePhone(Long userId, String newPhone, String unlockPassword) {
        if (!PHONE_PATTERN.matcher(newPhone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_INVALID_FORMAT);
        }

        PhoneBinding binding = phoneBindingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_BINDING_NOT_FOUND));

        if (!passwordEncoder.matches(unlockPassword, binding.getUnlockPassword())) {
            throw new BusinessException(ErrorCode.UNLOCK_PASSWORD_WRONG);
        }

        if (phoneBindingRepository.existsByPhone(newPhone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
        }

        AdminUser user = binding.getUser();
        user.setPhone(newPhone);
        adminUserRepository.save(user);

        binding.setPhone(newPhone);
        phoneBindingRepository.save(binding);

        log.info("Phone changed: userId={}, newPhone={}", userId, maskPhone(newPhone));
    }

    @Transactional
    public Map<String, String> requestVerifyCode(String phone, String unlockPassword) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_INVALID_FORMAT);
        }

        PhoneBinding binding = phoneBindingRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_BOUND));

        if (!passwordEncoder.matches(unlockPassword, binding.getUnlockPassword())) {
            throw new BusinessException(ErrorCode.UNLOCK_PASSWORD_WRONG);
        }

        // Generate 6-char code
        String codePlain = generateCode();
        binding.setVerifyCode(passwordEncoder.encode(codePlain));
        binding.setCodeExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        binding.setCodeAttemptCount(0);

        if (simulation) {
            phoneBindingRepository.save(binding);
            log.info("Verify code generated (simulation): phone={}", maskPhone(phone));
            return Map.of("code", codePlain, "expiresIn", String.valueOf(CODE_EXPIRE_MINUTES));
        } else {
            phoneBindingRepository.save(binding);
            log.info("Verify code generated: phone={}", maskPhone(phone));
            return Map.of("expiresIn", String.valueOf(CODE_EXPIRE_MINUTES));
        }
    }

    @Transactional
    public LoginResponse loginWithCode(String phone, String code, HttpServletRequest httpRequest) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_INVALID_FORMAT);
        }

        PhoneBinding binding = phoneBindingRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_BOUND));

        // Check expiry
        if (binding.getCodeExpireTime() == null || LocalDateTime.now().isAfter(binding.getCodeExpireTime())) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_EXPIRED);
        }

        // Check attempts
        if (binding.getCodeAttemptCount() != null && binding.getCodeAttemptCount() >= MAX_CODE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_MAX_ATTEMPTS);
        }

        // Verify code
        if (!passwordEncoder.matches(code, binding.getVerifyCode())) {
            binding.setCodeAttemptCount((binding.getCodeAttemptCount() == null ? 0 : binding.getCodeAttemptCount()) + 1);
            phoneBindingRepository.save(binding);
            throw new BusinessException(ErrorCode.VERIFY_CODE_WRONG);
        }

        // Success: clear code, generate JWT
        binding.setVerifyCode(null);
        binding.setCodeExpireTime(null);
        binding.setCodeAttemptCount(0);
        phoneBindingRepository.save(binding);

        AdminUser user = binding.getUser();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 重置锁定状态（密码登录失败锁定后，手机登录成功应解锁）
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setLockCount(0);
        adminUserRepository.save(user);

        var permissions = permissionService.getEffectivePermissions(user.getId(), user.getRole());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getId(), permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = ipExtractor.extractClientIp(httpRequest);
        sessionStore.storeToken(user.getId(), accessToken, refreshToken, userAgent, user.getRole(), ip, "手机验证码登录", true);

        log.info("Phone login success: userId={}, phone={}", user.getId(), maskPhone(phone));

        return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getName(), user.getAvatarUrl(), user.getRole(), permissions);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBindingStatus(Long userId) {
        Optional<PhoneBinding> binding = phoneBindingRepository.findByUserId(userId);
        if (binding.isPresent()) {
            PhoneBinding b = binding.get();
            return Map.of("bound", true, "phone", maskPhone(b.getPhone()));
        }
        return Map.of("bound", false);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
