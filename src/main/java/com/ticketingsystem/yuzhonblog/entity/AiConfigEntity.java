package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_config", indexes = {
    @Index(name = "idx_ai_config_active", columnList = "is_active")
})
public class AiConfigEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "api_key", nullable = false, columnDefinition = "TEXT")
    private String apiKey;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "max_tokens")
    private Integer maxTokens = 4096;

    @Column
    private Double temperature = 0.7;

    @Column(name = "api_format", length = 20)
    private String apiFormat = "OPENAI";

    @Column(name = "auth_type", length = 20)
    private String authType = "BEARER";

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "balance_url", length = 500)
    private String balanceUrl;

    @Column(name = "balance_script", columnDefinition = "TEXT")
    private String balanceScript;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
