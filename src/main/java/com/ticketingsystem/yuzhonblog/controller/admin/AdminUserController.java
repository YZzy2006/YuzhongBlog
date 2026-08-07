package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserCreateRequest;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserResponse;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserUpdateRequest;
import com.ticketingsystem.yuzhonblog.dto.admin.PasswordChangeRequest;
import com.ticketingsystem.yuzhonblog.dto.admin.PasswordResetRequest;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<PageResult<AdminUserResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 100);
        Page<AdminUserResponse> result = adminUserService.getUserList(keyword, page, size);
        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePermission("user:view")
    public ApiResponse<AdminUserResponse> get(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.getUser(id));
    }

    @PostMapping
    @RequirePermission("user:manage")
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody AdminUserCreateRequest request,
                                                  Authentication authentication) {
        String operatorRole = getOperatorRole(authentication);
        return ApiResponse.success(adminUserService.createUser(request, operatorRole));
    }

    @PutMapping("/{id}")
    @RequirePermission("user:manage")
    public ApiResponse<AdminUserResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody AdminUserUpdateRequest request,
                                                  Authentication authentication) {
        String operatorUsername = authentication.getName();
        String operatorRole = getOperatorRole(authentication);
        return ApiResponse.success(adminUserService.updateUser(id, request, operatorUsername, operatorRole));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("user:manage")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        String operatorUsername = authentication.getName();
        String operatorRole = getOperatorRole(authentication);
        adminUserService.deleteUser(id, operatorUsername, operatorRole);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle-enabled")
    @RequirePermission("user:manage")
    public ApiResponse<Void> toggleEnabled(@PathVariable Long id, Authentication authentication) {
        String operatorUsername = authentication.getName();
        String operatorRole = getOperatorRole(authentication);
        adminUserService.toggleUserEnabled(id, operatorUsername, operatorRole);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/reset-password")
    @RequirePermission("user:manage")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                            @Valid @RequestBody PasswordResetRequest request,
                                            Authentication authentication) {
        String operatorRole = getOperatorRole(authentication);
        adminUserService.resetPassword(id, request.getNewPassword(), operatorRole);
        return ApiResponse.success(null);
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request,
                                               Authentication authentication) {
        adminUserService.changeMyPassword(authentication.getName(), request);
        return ApiResponse.success(null);
    }

    private String getOperatorRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5).toLowerCase())
                .findFirst()
                .orElse("admin");
    }
}
