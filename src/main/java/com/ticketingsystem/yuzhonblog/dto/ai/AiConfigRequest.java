package com.ticketingsystem.yuzhonblog.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiConfigRequest {
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称不能超过100字")
    private String name;
    @Size(max = 500, message = "API Key 不能超过500字")
    private String apiKey;
    @NotBlank(message = "Base URL 不能为空")
    @Size(max = 500, message = "Base URL 不能超过500字")
    private String baseUrl;
    @NotBlank(message = "模型不能为空")
    @Size(max = 100, message = "模型名不能超过100字")
    private String model;
    private Integer maxTokens;
    private Double temperature;
    @Size(max = 50, message = "API格式不能超过50字")
    private String apiFormat;
    @Size(max = 50, message = "认证类型不能超过50字")
    private String authType;
    @Size(max = 500, message = "网站URL不能超过500字")
    private String websiteUrl;
    @Size(max = 500, message = "余额URL不能超过500字")
    private String balanceUrl;
    @Size(max = 2000, message = "余额脚本不能超过2000字")
    private String balanceScript;
    @Size(max = 500, message = "描述不能超过500字")
    private String description;
    private Integer sortOrder;
}
