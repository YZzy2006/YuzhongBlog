package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "weather_config", indexes = {
    @Index(name = "idx_weather_config_active", columnList = "is_active")
})
public class WeatherConfigEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "api_key", nullable = false, columnDefinition = "TEXT")
    private String apiKey;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_format", length = 20)
    private String apiFormat = "json";

    @Column(name = "auth_type", length = 20)
    private String authType = "query_param";

    @Column(length = 10)
    private String language = "zh";

    @Column(length = 10)
    private String units = "c";

    @Column(length = 100)
    private String location;

    @Column(name = "extra_params", columnDefinition = "TEXT")
    private String extraParams;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = false;
}
