package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.entity.LoginLog;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.repository.LoginLogRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionStore sessionStore;
    private final CurrentUserProvider currentUserProvider;
    private final AdminUserRepository adminUserRepository;
    private final LoginLogRepository loginLogRepository;
    private final IpExtractor ipExtractor;

    @GetMapping("/kick-notification")
    public ResponseEntity<ApiResponse<SessionStore.KickEvent>> getKickNotification() {
        Long userId = currentUserProvider.getCurrentUser().getId();
        SessionStore.SessionData currentSession = sessionStore.getTokenInfo(userId);

        SessionStore.KickEvent event = sessionStore.peekKickEvent(userId);
        if (event != null) {
            // Skip kick events created by the current session (new login kicking old session)
            if (currentSession != null && event.loginTime().equals(currentSession.loginTime())) {
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            // This is the kicked session — consume and return
            sessionStore.consumeKickEvent(userId);
            return ResponseEntity.ok(ApiResponse.success(event));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/freeze-account")
    @RequirePermission("user:manage")
    public ApiResponse<Void> freezeAccount(HttpServletRequest httpRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = currentUserProvider.getCurrentUser().getId();

        // 验证 token 匹配当前 session，防止被踢下线的旧 token 冻结账户
        String token = authHeader.substring(7);
        SessionStore.SessionData session = sessionStore.getTokenInfo(userId);
        if (session == null || !token.equals(session.token())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if ("super_admin".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        user.setEnabled(false);
        adminUserRepository.save(user);
        sessionStore.removeToken(userId);

        // Record freeze action in login log for audit
        try {
            LoginLog logEntry = new LoginLog();
            logEntry.setUserId(userId);
            logEntry.setUsername(user.getUsername());
            logEntry.setLoginIp(ipExtractor.extractClientIp(httpRequest));
            logEntry.setUserAgent(httpRequest.getHeader("User-Agent"));
            logEntry.setDeviceInfo(SessionStore.parseDeviceInfo(httpRequest.getHeader("User-Agent")));
            logEntry.setStatus(0);
            logEntry.setFailReason("用户主动冻结账号");
            loginLogRepository.save(logEntry);
        } catch (Exception e) {
            // log save failure shouldn't block the freeze
        }

        return ApiResponse.success(null);
    }
}
