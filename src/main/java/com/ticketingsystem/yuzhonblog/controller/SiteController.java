package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.site.SiteInfoResponse;
import com.ticketingsystem.yuzhonblog.service.SiteSettingService;
import com.ticketingsystem.yuzhonblog.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteSettingService siteSettingService;
    private final WeatherService weatherService;

    @GetMapping("/info")
    public ApiResponse<SiteInfoResponse> siteInfo() {
        return ApiResponse.success(siteSettingService.getSiteInfo());
    }

    @GetMapping("/weather")
    public Map<String, Object> weather(@RequestParam double lat, @RequestParam double lon) {
        return weatherService.fetchWeather(lat, lon);
    }
}
