package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.admin.OssConfigRequest;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.OssConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/oss")
@RequiredArgsConstructor
@Slf4j
public class OssConfigAdminController {

    private final OssConfigService ossConfigService;

    @GetMapping("/config")
    @RequirePermission("setting:view")
    public ApiResponse<Map<String, String>> getConfig() {
        return ApiResponse.success(ossConfigService.getConfig());
    }

    @PutMapping("/config")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> saveConfig(@Valid @RequestBody OssConfigRequest config) {
        ossConfigService.saveConfig(config);
        log.info("OSS配置已通过管理后台更新");
        return ApiResponse.success();
    }

    @PostMapping("/test")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> testConnection() {
        Map<String, Object> result = ossConfigService.testConnection();
        log.info("OSS连接测试: success={}", result.get("success"));
        return ApiResponse.success(result);
    }
}
