package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementCreateRequest;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementResponse;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementUpdateRequest;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.AnnouncementService;
import com.ticketingsystem.yuzhonblog.service.AnnouncementTranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AnnouncementAdminController {

    private final AnnouncementService announcementService;
    private final AnnouncementTranslationService announcementTranslationService;

    @GetMapping
    @RequirePermission("announcement:view")
    public ApiResponse<?> list(@RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) Boolean active,
                               @RequestParam(required = false) String sort) {
        if (page != null && size != null) {
            return ApiResponse.success(announcementService.getAdminPage(page, size, keyword, type, active, sort));
        }
        return ApiResponse.success(announcementService.getAdminList());
    }

    @GetMapping("/{id}")
    @RequirePermission("announcement:view")
    public ApiResponse<AnnouncementResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(announcementService.getById(id));
    }

    @PostMapping
    @RequirePermission("announcement:manage")
    public ApiResponse<AnnouncementResponse> create(@Valid @RequestBody AnnouncementCreateRequest request) {
        return ApiResponse.success(announcementService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("announcement:manage")
    public ApiResponse<AnnouncementResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AnnouncementUpdateRequest request) {
        return ApiResponse.success(announcementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("announcement:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/translate")
    @RequirePermission("announcement:manage")
    public ApiResponse<String> translateAll() {
        int count = announcementTranslationService.translateAllPending();
        return ApiResponse.success("Queued " + count + " announcements for translation");
    }

    @PatchMapping("/{id}/toggle")
    @RequirePermission("announcement:manage")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        announcementService.toggleActive(id);
        return ApiResponse.success();
    }

    @DeleteMapping
    @RequirePermission("announcement:manage")
    public ApiResponse<Void> bulkDelete(@RequestParam List<Long> ids) {
        announcementService.bulkDelete(ids);
        return ApiResponse.success();
    }
}
