package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.site.SiteSettingRequest;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SiteSettingAdminController {

    private final SiteSettingService siteSettingService;

    @GetMapping
    @RequirePermission("setting:view")
    public ApiResponse<Map<String, String>> list() {
        return ApiResponse.success(siteSettingService.getAllSettings());
    }

    @PutMapping
    @RequirePermission("setting:edit")
    public ApiResponse<Void> update(@Valid @RequestBody SiteSettingRequest request) {
        siteSettingService.updateSettings(request);
        return ApiResponse.success();
    }
}
