package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "login_log", indexes = {
    @Index(name = "idx_login_log_user_status_time", columnList = "user_id, status, created_at"),
    @Index(name = "idx_login_log_status_time", columnList = "status, created_at"),
    @Index(name = "idx_login_log_username_status", columnList = "username, status, created_at"),
    @Index(name = "idx_login_log_created", columnList = "created_at")
})
public class LoginLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(name = "login_ip", length = 50)
    private String loginIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 200)
    private String deviceInfo;

    /** 1=成功, 0=失败 */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(length = 100)
    private String location;
}
