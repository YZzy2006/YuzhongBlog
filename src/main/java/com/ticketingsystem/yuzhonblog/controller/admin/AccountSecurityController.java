package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.account.AccountSecurityResponse;
import com.ticketingsystem.yuzhonblog.dto.account.SessionInfo;
import com.ticketingsystem.yuzhonblog.dto.log.HeatmapEntry;
import com.ticketingsystem.yuzhonblog.dto.log.SecurityAlert;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import com.ticketingsystem.yuzhonblog.service.LoginLogService;
import com.ticketingsystem.yuzhonblog.service.PhoneLoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/account-security")
@RequiredArgsConstructor
public class AccountSecurityController {

    private final LoginLogService loginLogService;
    private final PhoneLoginService phoneLoginService;
    private final CurrentUserProvider currentUserProvider;
    private final IpExtractor ipExtractor;

    @GetMapping
    public ApiResponse<AccountSecurityResponse> getAccountSecurity(HttpServletRequest request) {
        AdminUser user = currentUserProvider.getCurrentUser();
        Long userId = user.getId();

        // Current session
        SessionStore.SessionData session = loginLogService.getCurrentSession(userId);
        String clientIp = ipExtractor.extractClientIp(request);
        SessionInfo sessionInfo = session != null
                ? new SessionInfo(session.deviceInfo(), session.loginTime(), clientIp)
                : new SessionInfo("未知设备", "未知", clientIp);

        // Heatmap
        List<HeatmapEntry> heatmap = loginLogService.getLoginHeatmap(userId);

        // Phone binding
        Map<String, Object> phoneStatus = phoneLoginService.getBindingStatus(userId);
        boolean phoneBound = (Boolean) phoneStatus.getOrDefault("bound", false);
        String maskedPhone = (String) phoneStatus.get("phone");

        // Security alerts (for super_admin)
        List<SecurityAlert> alerts = "super_admin".equals(user.getRole())
                ? loginLogService.getSecurityAlerts()
                : List.of();

        AccountSecurityResponse resp = new AccountSecurityResponse();
        resp.setCurrentSession(sessionInfo);
        resp.setHeatmap(heatmap);
        resp.setPhoneBound(phoneBound);
        resp.setMaskedPhone(maskedPhone);
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setSecurityAlerts(alerts);

        return ApiResponse.success(resp);
    }
}
