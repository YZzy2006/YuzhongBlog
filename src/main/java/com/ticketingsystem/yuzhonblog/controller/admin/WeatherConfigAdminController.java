package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.weather.WeatherConfigRequest;
import com.ticketingsystem.yuzhonblog.entity.WeatherConfigEntity;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.WeatherConfigService;
import com.ticketingsystem.yuzhonblog.service.WeatherTestService;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherConfigAdminController {

    private final WeatherConfigService weatherConfigService;
    private final WeatherTestService weatherTestService;
    private final AesUtil aesUtil;

    @GetMapping("/configs")
    @RequirePermission("setting:view")
    public ApiResponse<List<Map<String, Object>>> listConfigs() {
        List<WeatherConfigEntity> configs = weatherConfigService.list();
        List<Map<String, Object>> result = configs.stream().map(this::toMaskedResponse).toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/configs/{id}")
    @RequirePermission("setting:view")
    public ApiResponse<Map<String, Object>> getConfig(@PathVariable Long id) {
        WeatherConfigEntity entity = weatherConfigService.get(id);
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @PostMapping("/configs")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> createConfig(@Valid @RequestBody WeatherConfigRequest request) {
        WeatherConfigEntity entity = weatherConfigService.create(request);
        log.info("天气配置已创建: id={}", entity.getId());
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @PutMapping("/configs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> updateConfig(@PathVariable Long id, @Valid @RequestBody WeatherConfigRequest request) {
        WeatherConfigEntity entity = weatherConfigService.update(id, request);
        log.info("天气配置已更新: id={}", id);
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @DeleteMapping("/configs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        weatherConfigService.delete(id);
        log.info("天气配置已删除: id={}", id);
        return ApiResponse.success();
    }

    @PutMapping("/configs/{id}/activate")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> activateConfig(@PathVariable Long id) {
        weatherConfigService.activate(id);
        log.info("天气配置已激活: id={}", id);
        return ApiResponse.success();
    }

    @PostMapping("/configs/{id}/test")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> testConnection(@PathVariable Long id) {
        WeatherConfigEntity entity = weatherConfigService.get(id);
        Map<String, Object> result = weatherTestService.testConnection(entity);
        log.info("天气连接测试: id={}, success={}", id, result.get("success"));
        return ApiResponse.success(result);
    }

    // ==================== Helpers ====================

    private Map<String, Object> toMaskedResponse(WeatherConfigEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        map.put("provider", entity.getProvider());
        map.put("apiKey", maskApiKey(decryptKey(entity)));
        map.put("baseUrl", entity.getBaseUrl());
        map.put("apiFormat", entity.getApiFormat());
        map.put("authType", entity.getAuthType());
        map.put("language", entity.getLanguage());
        map.put("units", entity.getUnits());
        map.put("location", entity.getLocation());
        map.put("extraParams", entity.getExtraParams());
        map.put("description", entity.getDescription());
        map.put("isActive", entity.getIsActive());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    private String decryptKey(WeatherConfigEntity entity) {
        try {
            String decrypted = aesUtil.decrypt(entity.getApiKey());
            return decrypted != null ? decrypted : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) return "****";
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
