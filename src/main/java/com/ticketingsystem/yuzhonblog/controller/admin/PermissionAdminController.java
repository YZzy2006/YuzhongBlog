package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.admin.PermissionUpdateRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import com.ticketingsystem.yuzhonblog.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionService permissionService;
    private final AdminUserRepository adminUserRepository;
    private final SessionStore sessionStore;

    /**
     * Get permission matrix for a user.
     */
    @GetMapping("/users/{userId}")
    @RequirePermission("user:manage")
    public ApiResponse<List<Map<String, Object>>> getUserPermissions(@PathVariable Long userId) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ApiResponse.success(permissionService.getPermissionMatrix(user.getId(), user.getRole()));
    }

    /**
     * Batch update permissions for a user.
     */
    @PutMapping("/users/{userId}")
    @RequirePermission("user:manage")
    public ApiResponse<Void> updateUserPermissions(@PathVariable Long userId,
                                                    @Valid @RequestBody PermissionUpdateRequest request,
                                                    Authentication authentication) {
        AdminUser targetUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Cannot modify super_admin permissions
        if ("super_admin".equals(targetUser.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "超级管理员权限不可修改");
        }
        // Cannot modify visitor permissions (fixed)
        if ("visitor".equals(targetUser.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "访客权限不可修改");
        }

        // Defense-in-depth: prevent self-modification
        AdminUser currentUser = adminUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "不能修改自己的权限");
        }

        Map<String, Boolean> permMap = request.getPermissions().stream()
                .collect(Collectors.toMap(
                        PermissionUpdateRequest.PermissionItem::getPermission,
                        PermissionUpdateRequest.PermissionItem::getEnabled));
        permissionService.setUserPermissions(userId, permMap);

        // Force target user to re-login so frontend picks up new permissions
        sessionStore.removeToken(userId);
        return ApiResponse.success(null);
    }
}
