package com.ticketingsystem.yuzhonblog.dto.weather;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WeatherConfigRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称不能超过100字")
    private String name;

    @NotBlank(message = "供应商不能为空")
    @Size(max = 50, message = "供应商不能超过50字")
    private String provider;

    @Size(max = 500, message = "API Key 不能超过500字")
    private String apiKey;

    @NotBlank(message = "API地址不能为空")
    @Size(max = 500, message = "API地址不能超过500字")
    private String baseUrl;

    @Size(max = 20, message = "API格式不能超过20字")
    private String apiFormat;

    @Size(max = 20, message = "认证方式不能超过20字")
    private String authType;

    @Size(max = 10, message = "语言不能超过10字")
    private String language;

    @Size(max = 10, message = "单位不能超过10字")
    private String units;

    @Size(max = 100, message = "位置不能超过100字")
    private String location;

    @Size(max = 10000, message = "附加参数不能超过10000字")
    private String extraParams;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;
}
