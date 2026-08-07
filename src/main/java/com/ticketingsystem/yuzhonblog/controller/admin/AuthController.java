package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginRequest;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.RefreshRequest;
import com.ticketingsystem.yuzhonblog.service.AuthService;
import com.ticketingsystem.yuzhonblog.util.CaptchaUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaUtil captchaUtil;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.refreshToken(request, httpRequest));
    }

    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> captcha() {
        return ApiResponse.success(captchaUtil.generate());
    }
}
