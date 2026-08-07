package com.ticketingsystem.yuzhonblog.common;

import java.util.List;
import java.util.Map;

public enum Permission {
    // 文章
    ARTICLE_VIEW("article:view", "查看文章", "文章"),
    ARTICLE_CREATE("article:create", "创建文章", "文章"),
    ARTICLE_EDIT("article:edit", "编辑文章", "文章"),
    ARTICLE_PUBLISH("article:publish", "发布文章", "文章"),
    ARTICLE_DELETE("article:delete", "删除文章", "文章"),

    // 分类
    CATEGORY_VIEW("category:view", "查看分类", "分类"),
    CATEGORY_MANAGE("category:manage", "管理分类", "分类"),

    // 标签
    TAG_VIEW("tag:view", "查看标签", "标签"),
    TAG_MANAGE("tag:manage", "管理标签", "标签"),

    // 项目
    PROJECT_VIEW("project:view", "查看项目", "项目"),
    PROJECT_MANAGE("project:manage", "管理项目", "项目"),

    // 公告
    ANNOUNCEMENT_VIEW("announcement:view", "查看公告", "公告"),
    ANNOUNCEMENT_MANAGE("announcement:manage", "管理公告", "公告"),

    // 用户
    USER_VIEW("user:view", "查看用户", "用户"),
    USER_MANAGE("user:manage", "管理用户", "用户"),

    // 上传
    UPLOAD("upload:file", "上传文件", "文件"),

    // 内容审核
    REVIEW_VIEW("review:view", "查看审核记录", "审核"),
    REVIEW_MANAGE("review:manage", "管理审核", "审核"),

    // 照片墙
    PHOTOWALL_VIEW("photowall:view", "查看照片墙", "照片墙"),
    PHOTOWALL_MANAGE("photowall:manage", "管理照片墙", "照片墙");

    private final String code;
    private final String label;
    private final String group;

    Permission(String code, String label, String group) {
        this.code = code;
        this.label = label;
        this.group = group;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getGroup() { return group; }

    public boolean isViewPermission() {
        return code.endsWith(":view");
    }

    /**
     * Default permissions for admin role (all enabled).
     * Super admin always has all permissions.
     * Visitor only has *_VIEW permissions.
     */
    public static List<Permission> defaultsForRole(String role) {
        return switch (role) {
            case "super_admin" -> List.of(Permission.values());
            case "visitor" -> java.util.Arrays.stream(Permission.values())
                    .filter(Permission::isViewPermission).toList();
            default -> java.util.Arrays.stream(Permission.values())
                    .filter(p -> p != REVIEW_MANAGE && p != PHOTOWALL_VIEW && p != PHOTOWALL_MANAGE).toList(); // admin: all except review:manage, photowall
        };
    }

    /**
     * Returns permissions grouped by module for UI display.
     */
    public static Map<String, List<Permission>> grouped() {
        return java.util.Arrays.stream(values())
                .collect(java.util.stream.Collectors.groupingBy(Permission::getGroup));
    }
}
