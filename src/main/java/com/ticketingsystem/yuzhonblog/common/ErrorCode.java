package com.ticketingsystem.yuzhonblog.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 通用
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证相关 1xxx
    LOGIN_FAILED(1001, "用户名或密码错误"),
    TOKEN_EXPIRED(1002, "Token已过期"),
    TOKEN_INVALID(1003, "Token无效"),
    ACCOUNT_LOCKED(1004, "账号已被锁定"),
    SESSION_CONFLICT(1006, "账号已在其他设备登录"),
    CAPTCHA_REQUIRED(1007, "请输入验证码"),
    CAPTCHA_INVALID(1008, "验证码错误"),
    ACCOUNT_FROZEN(1009, "账号已被冻结"),

    // 用户管理相关 11xx
    USERNAME_EXISTS(1101, "用户名已存在"),
    INVALID_ROLE(1102, "无效的角色"),
    USER_NOT_FOUND(1103, "用户不存在"),
    CANNOT_DELETE_SELF(1104, "不能删除自己的账号"),
    CANNOT_DISABLE_SELF(1105, "不能禁用自己的账号"),
    CANNOT_MODIFY_SELF_ROLE(1106, "不能修改自己的角色"),
    OLD_PASSWORD_WRONG(1107, "原密码错误"),

    // 文章相关 2xxx
    ARTICLE_NOT_FOUND(2001, "文章不存在"),
    ARTICLE_SLUG_DUPLICATE(2002, "文章别名已存在"),

    // 分类/标签 3xxx
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_HAS_ARTICLES(3002, "该分类下还有文章"),
    TAG_NOT_FOUND(3003, "标签不存在"),

    // 项目 4xxx
    PROJECT_NOT_FOUND(4001, "项目不存在"),

    // 公告 5xxx
    ANNOUNCEMENT_NOT_FOUND(5001, "公告不存在"),

    // 文件上传 6xxx
    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(6002, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(6003, "文件大小超出限制"),

    // AI相关 7xxx
    AI_NOT_CONFIGURED(7001, "AI服务未配置"),
    AI_CALL_FAILED(7002, "AI服务调用失败，请稍后再试"),
    AI_RATE_LIMITED(7003, "AI请求过于频繁，请稍后再试"),
    RATE_LIMITED(7004, "请求过于频繁，请稍后再试"),

    // 天气相关 71xx
    WEATHER_NOT_CONFIGURED(7101, "天气服务未配置"),
    WEATHER_TEST_FAILED(7102, "天气API测试失败"),

    // 内容安全 8xxx
    SENSITIVE_WORD_BLOCKED(8001, "消息包含违规内容，请修改后重试"),
    REVIEW_PENDING(8002, "内容已提交AI审核，请等待管理员审批"),

    // 手机登录 12xx
    PHONE_ALREADY_BOUND(1201, "该手机号已被绑定"),
    PHONE_NOT_BOUND(1202, "该手机号未绑定账号"),
    PHONE_INVALID_FORMAT(1203, "手机号格式不正确"),
    UNLOCK_PASSWORD_WRONG(1204, "解锁密码错误"),
    VERIFY_CODE_EXPIRED(1205, "验证码已过期"),
    VERIFY_CODE_WRONG(1206, "验证码错误"),
    VERIFY_CODE_MAX_ATTEMPTS(1207, "验证码错误次数过多，请重新获取"),
    PHONE_BINDING_NOT_FOUND(1208, "未绑定手机号"),

    // 日志相关 13xx
    LOG_EXPORT_FAILED(1301, "日志导出失败"),

    // 照片墙 9xxx
    ALBUM_NOT_FOUND(9001, "相册不存在"),
    ALBUM_NAME_DUPLICATE(9002, "相册名已存在"),
    PHOTO_NOT_FOUND(9003, "照片不存在"),

    // 时间线 10xxx
    TIMELINE_ENTRY_NOT_FOUND(10001, "时间线条目不存在"),

    // 友链 11xxx
    FRIEND_LINK_NOT_FOUND(11001, "友链不存在"),

    // 备份相关 14xx
    BACKUP_NOT_FOUND(1401, "备份记录不存在"),
    BACKUP_FILE_NOT_FOUND(1402, "备份文件不存在"),
    BACKUP_IMPORT_INVALID(1403, "备份文件格式无效"),
    BACKUP_IMPORT_FAILED(1404, "备份导入失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
