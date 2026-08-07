package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminLogoutController {

    private final AuthService authService;
    private final AdminUserRepository adminUserRepository;

    @PostMapping("/admin/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        if (authentication != null) {
            adminUserRepository.findByUsername(authentication.getName())
                    .ifPresent(user -> authService.logout(user.getId()));
        }
        return ApiResponse.success(null);
    }
}
