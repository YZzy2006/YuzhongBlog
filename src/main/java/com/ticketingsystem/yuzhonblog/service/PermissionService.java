package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.common.Permission;
import com.ticketingsystem.yuzhonblog.entity.UserPermission;
import com.ticketingsystem.yuzhonblog.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserPermissionRepository permissionRepository;

    /**
     * Get all effective permissions for a user.
     * - super_admin: all permissions (not stored in DB)
     * - visitor: only VIEW permissions (not stored in DB)
     * - admin: all permissions minus explicitly disabled ones
     */
    @Transactional(readOnly = true)
    public List<String> getEffectivePermissions(Long userId, String role) {
        if ("super_admin".equals(role)) {
            return Arrays.stream(Permission.values()).map(Permission::getCode).toList();
        }
        if ("visitor".equals(role)) {
            return Arrays.stream(Permission.values())
                    .filter(Permission::isViewPermission)
                    .map(Permission::getCode).toList();
        }
        // admin: start with all permissions, remove disabled ones
        Set<String> allPerms = Arrays.stream(Permission.values())
                .map(Permission::getCode).collect(Collectors.toSet());
        List<UserPermission> overrides = permissionRepository.findByUserId(userId);
        for (UserPermission up : overrides) {
            if (!up.getEnabled()) {
                allPerms.remove(up.getPermission());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(allPerms));
    }

    /**
     * Check if a user has a specific permission.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String role, String permission) {
        if ("super_admin".equals(role)) return true;
        if ("visitor".equals(role)) {
            return Arrays.stream(Permission.values())
                    .filter(p -> p.getCode().equals(permission))
                    .anyMatch(Permission::isViewPermission);
        }
        // admin: check if explicitly disabled
        return !permissionRepository.isPermissionDisabled(userId, permission);
    }

    /**
     * Get full permission matrix for a user (for UI display).
     * Returns list of {code, label, group, enabled}.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPermissionMatrix(Long userId, String role) {
        if ("super_admin".equals(role)) {
            return Arrays.stream(Permission.values()).map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", p.getCode());
                m.put("label", p.getLabel());
                m.put("group", p.getGroup());
                m.put("enabled", true);
                m.put("locked", true); // super_admin cannot be changed
                return m;
            }).toList();
        }
        if ("visitor".equals(role)) {
            return Arrays.stream(Permission.values()).map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", p.getCode());
                m.put("label", p.getLabel());
                m.put("group", p.getGroup());
                m.put("enabled", p.isViewPermission());
                m.put("locked", true); // visitor cannot be changed
                return m;
            }).toList();
        }
        // admin: check overrides
        Map<String, Boolean> overrides = permissionRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserPermission::getPermission, UserPermission::getEnabled));
        return Arrays.stream(Permission.values()).map(p -> {
            boolean enabled = overrides.getOrDefault(p.getCode(), true);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", p.getCode());
            m.put("label", p.getLabel());
            m.put("group", p.getGroup());
            m.put("enabled", enabled);
            m.put("locked", false);
            return m;
        }).toList();
    }

    /**
     * Batch update permissions for a user (super_admin only).
     */
    @Transactional
    public void setUserPermissions(Long userId, Map<String, Boolean> permissions) {
        // Only store disabled overrides (admin has all permissions by default)
        List<UserPermission> toSave = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String permCode = entry.getKey();
            boolean enabled = entry.getValue();
            // Validate permission code
            if (Arrays.stream(Permission.values()).noneMatch(p -> p.getCode().equals(permCode))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "无效的权限: " + permCode);
            }
            Optional<UserPermission> existing = permissionRepository.findByUserIdAndPermission(userId, permCode);
            if (existing.isPresent()) {
                UserPermission up = existing.get();
                up.setEnabled(enabled);
                toSave.add(up);
            } else {
                UserPermission up = new UserPermission();
                up.setUserId(userId);
                up.setPermission(permCode);
                up.setEnabled(enabled);
                toSave.add(up);
            }
        }
        permissionRepository.saveAll(toSave);
        log.info("Updated permissions for userId={}: {} entries", userId, permissions.size());
    }

    /**
     * Clear all permission overrides for a user (used when role changes).
     */
    @Transactional
    public void clearUserPermissions(Long userId) {
        permissionRepository.deleteByUserId(userId);
        log.info("Cleared permission overrides for userId={}", userId);
    }
}
