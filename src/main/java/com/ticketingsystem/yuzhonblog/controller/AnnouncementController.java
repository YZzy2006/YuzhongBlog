package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementResponse;
import com.ticketingsystem.yuzhonblog.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ApiResponse<List<AnnouncementResponse>> list() {
        return ApiResponse.success(announcementService.getPublicList());
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<AnnouncementResponse>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "default") String sort) {
        size = Math.min(size, 100);
        return ApiResponse.success(announcementService.getPublicPage(page, size, keyword, type, tag, sort));
    }
}
