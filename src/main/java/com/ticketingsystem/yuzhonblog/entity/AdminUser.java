package com.ticketingsystem.yuzhonblog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admin_user")
public class AdminUser extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(length = 50)
    private String realName;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String role = "admin";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    @Column(name = "lock_count")
    private Integer lockCount = 0;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String avatarUrl;
}
