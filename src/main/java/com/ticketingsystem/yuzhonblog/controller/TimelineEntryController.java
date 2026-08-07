package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.timeline.TimelineEntryResponse;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.service.TimelineEntryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeline-entries")
@RequiredArgsConstructor
public class TimelineEntryController {

    private final TimelineEntryService timelineEntryService;
    private final IpExtractor ipExtractor;

    @GetMapping
    public ApiResponse<List<TimelineEntryResponse>> list() {
        return ApiResponse.success(timelineEntryService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<TimelineEntryResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(timelineEntryService.getDetailById(id));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id, HttpServletRequest request) {
        String ip = ipExtractor.extractClientIp(request);
        return ApiResponse.success(timelineEntryService.like(id, ip));
    }

    @GetMapping("/{id}/liked")
    public ApiResponse<Map<String, Boolean>> isLiked(@PathVariable Long id, HttpServletRequest request) {
        String ip = ipExtractor.extractClientIp(request);
        return ApiResponse.success(Map.of("liked", timelineEntryService.isLikedByIp(id, ip)));
    }
}
