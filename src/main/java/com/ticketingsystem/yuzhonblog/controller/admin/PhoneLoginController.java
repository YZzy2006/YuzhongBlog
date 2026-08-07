package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.PhoneBindRequest;
import com.ticketingsystem.yuzhonblog.dto.auth.PhoneChangeRequest;
import com.ticketingsystem.yuzhonblog.dto.auth.PhoneLoginRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.PhoneLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/phone")
@RequiredArgsConstructor
public class PhoneLoginController {

    private final PhoneLoginService phoneLoginService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/status")
    @RequirePermission("user:manage")
    public ApiResponse<Map<String, Object>> status() {
        AdminUser user = currentUserProvider.getCurrentUser();
        return ApiResponse.success(phoneLoginService.getBindingStatus(user.getId()));
    }

    @PostMapping("/bind")
    @RequirePermission("user:manage")
    public ApiResponse<Void> bind(@Valid @RequestBody PhoneBindRequest body) {
        AdminUser user = currentUserProvider.getCurrentUser();
        phoneLoginService.bindPhone(user.getId(), body.getPhone(), body.getUnlockPassword());
        return ApiResponse.success(null);
    }

    @PostMapping("/change")
    @RequirePermission("user:manage")
    public ApiResponse<Void> change(@Valid @RequestBody PhoneChangeRequest body) {
        AdminUser user = currentUserProvider.getCurrentUser();
        phoneLoginService.changePhone(user.getId(), body.getNewPhone(), body.getUnlockPassword());
        return ApiResponse.success(null);
    }

    @PostMapping("/request-code")
    public ApiResponse<Map<String, String>> requestCode(@Valid @RequestBody PhoneBindRequest body) {
        return ApiResponse.success(phoneLoginService.requestVerifyCode(body.getPhone(), body.getUnlockPassword()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody PhoneLoginRequest body,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(phoneLoginService.loginWithCode(body.getPhone(), body.getCode(), httpRequest));
    }
}
