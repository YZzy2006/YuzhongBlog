package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserCreateRequest;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserResponse;
import com.ticketingsystem.yuzhonblog.dto.admin.AdminUserUpdateRequest;
import com.ticketingsystem.yuzhonblog.dto.admin.PasswordChangeRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;
    private final PermissionService permissionService;

    private static final String SUPER_ADMIN = "super_admin";
    private static final String VISITOR = "visitor";

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUserList(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminUserRepository.findByKeyword(keyword, pageRequest).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request, String operatorRole) {
        // 只有 super_admin 可以创建用户
        if (!SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 检查用户名唯一
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 角色校验：允许 admin、super_admin、visitor
        String role = request.getRole();
        if (role == null || role.isBlank()) {
            role = "admin";
        }
        if (!role.equals("admin") && !role.equals(SUPER_ADMIN) && !role.equals(VISITOR)) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        AdminUser user = new AdminUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setEnabled(true);

        return AdminUserResponse.from(adminUserRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request,
                                        String operatorUsername, String operatorRole) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 不能编辑 super_admin（除非自己是 super_admin）
        if (SUPER_ADMIN.equals(user.getRole()) && !SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 普通 admin 不能修改角色
        if (request.getRole() != null && !SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 角色校验
        if (request.getRole() != null) {
            String newRole = request.getRole();
            if (!newRole.equals("admin") && !newRole.equals(SUPER_ADMIN) && !newRole.equals(VISITOR)) {
                throw new BusinessException(ErrorCode.INVALID_ROLE);
            }
            // 不能把自己的角色改掉
            if (user.getUsername().equals(operatorUsername)) {
                throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF_ROLE);
            }
            // 角色变更时清空自定义权限（新角色将使用默认权限）
            if (!user.getRole().equals(newRole)) {
                permissionService.clearUserPermissions(userId);
            }
            user.setRole(newRole);
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        return AdminUserResponse.from(adminUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, String operatorUsername, String operatorRole) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 不能删除自己
        if (user.getUsername().equals(operatorUsername)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }

        // 不能删除 super_admin（除非自己是 super_admin）
        if (SUPER_ADMIN.equals(user.getRole()) && !SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 清除被删用户的会话和权限
        sessionStore.removeToken(userId);
        permissionService.clearUserPermissions(userId);
        adminUserRepository.delete(user);
    }

    @Transactional
    public void toggleUserEnabled(Long userId, String operatorUsername, String operatorRole) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 不能禁用自己
        if (user.getUsername().equals(operatorUsername)) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }

        // 不能禁用 super_admin
        if (SUPER_ADMIN.equals(user.getRole()) && !SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 普通管理员之间不能互相操作（同级）
        if (!SUPER_ADMIN.equals(operatorRole) && !SUPER_ADMIN.equals(user.getRole())
                && user.getRole().equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        adminUserRepository.save(user);

        // 禁用时清除会话，强制下线
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            sessionStore.removeToken(userId);
        }
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword, String operatorRole) {
        if (!SUPER_ADMIN.equals(operatorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setLockCount(0);
        adminUserRepository.save(user);

        // 清除会话，强制重新登录
        sessionStore.removeToken(userId);
    }

    @Transactional
    public void changeMyPassword(String username, PasswordChangeRequest request) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setLockCount(0);
        adminUserRepository.save(user);

        // 清除会话，强制重新登录
        sessionStore.removeToken(user.getId());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return AdminUserResponse.from(user);
    }
}
