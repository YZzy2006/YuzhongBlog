package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.timeline.TimelineEntryRequest;
import com.ticketingsystem.yuzhonblog.dto.timeline.TimelineEntryResponse;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.TimelineEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/timeline-entries")
@RequiredArgsConstructor
public class TimelineEntryAdminController {

    private final TimelineEntryService timelineEntryService;

    @GetMapping
    @RequirePermission("project:view")
    public ApiResponse<PageResult<TimelineEntryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "default") String sort) {
        return ApiResponse.success(timelineEntryService.getAdminList(page, size, keyword, status, sort));
    }

    @GetMapping("/{id}")
    @RequirePermission("project:view")
    public ApiResponse<TimelineEntryResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(timelineEntryService.getById(id));
    }

    @PostMapping
    @RequirePermission("project:manage")
    public ApiResponse<TimelineEntryResponse> create(@Valid @RequestBody TimelineEntryRequest request) {
        return ApiResponse.success(timelineEntryService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<TimelineEntryResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody TimelineEntryRequest request) {
        return ApiResponse.success(timelineEntryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("project:manage")
    public ApiResponse<TimelineEntryResponse> toggleStatus(@PathVariable Long id) {
        return ApiResponse.success(timelineEntryService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        timelineEntryService.delete(id);
        return ApiResponse.success();
    }
}
