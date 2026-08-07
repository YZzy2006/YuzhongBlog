package com.ticketingsystem.yuzhonblog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@BatchSize(size = 10)
@Table(name = "phone_binding")
public class PhoneBinding extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private AdminUser user;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @JsonIgnore
    @Column(name = "unlock_password", nullable = false, length = 100)
    private String unlockPassword;

    @JsonIgnore
    @Column(name = "verify_code", length = 100)
    private String verifyCode;

    @Column(name = "code_expire_time")
    private LocalDateTime codeExpireTime;

    @Column(name = "code_attempt_count")
    private Integer codeAttemptCount = 0;

    @Version
    private Long version;
}
