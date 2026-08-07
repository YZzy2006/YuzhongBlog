package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.photowall.*;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.PhotoWallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/photowall")
@RequiredArgsConstructor
public class PhotoWallAdminController {

    private final PhotoWallService photoWallService;

    // ==================== Album ====================

    @GetMapping("/albums")
    @RequirePermission("photowall:view")
    public ApiResponse<PageResult<PhotoAlbumResponse>> listAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ApiResponse.success(photoWallService.listAlbums(true, page, size));
    }

    @GetMapping("/albums/{id}")
    @RequirePermission("photowall:view")
    public ApiResponse<PhotoAlbumDetailResponse> getAlbumDetail(@PathVariable Long id) {
        return ApiResponse.success(photoWallService.getAlbumDetail(id, true));
    }

    @PostMapping("/albums")
    @RequirePermission("photowall:manage")
    public ApiResponse<PhotoAlbumResponse> createAlbum(@Valid @RequestBody PhotoAlbumRequest request) {
        return ApiResponse.success(photoWallService.createAlbum(request));
    }

    @PutMapping("/albums/{id}")
    @RequirePermission("photowall:manage")
    public ApiResponse<PhotoAlbumResponse> updateAlbum(
            @PathVariable Long id,
            @Valid @RequestBody PhotoAlbumRequest request) {
        return ApiResponse.success(photoWallService.updateAlbum(id, request));
    }

    @DeleteMapping("/albums/{id}")
    @RequirePermission("photowall:manage")
    public ApiResponse<Void> deleteAlbum(@PathVariable Long id) {
        photoWallService.deleteAlbum(id);
        return ApiResponse.success();
    }

    // ==================== Photo ====================

    @PostMapping("/photos")
    @RequirePermission("photowall:manage")
    public ApiResponse<PhotoResponse> addPhoto(@Valid @RequestBody PhotoRequest request) {
        return ApiResponse.success(photoWallService.addPhoto(request));
    }

    @DeleteMapping("/photos/{id}")
    @RequirePermission("photowall:manage")
    public ApiResponse<Void> deletePhoto(@PathVariable Long id) {
        photoWallService.deletePhoto(id);
        return ApiResponse.success();
    }

    @PutMapping("/photos/{id}/caption")
    @RequirePermission("photowall:manage")
    public ApiResponse<PhotoResponse> updatePhotoCaption(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String caption = body.get("caption");
        if (caption != null && caption.length() > 200) {
            throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                    com.ticketingsystem.yuzhonblog.common.ErrorCode.BAD_REQUEST);
        }
        return ApiResponse.success(photoWallService.updatePhotoCaption(id, caption));
    }

    @PostMapping("/albums/{id}/photos/batch")
    @RequirePermission("photowall:manage")
    public ApiResponse<Void> batchAddPhotos(
            @PathVariable Long id,
            @RequestBody @jakarta.validation.constraints.Size(max = 100, message = "单次最多批量添加100张") List<String> urls) {
        for (String url : urls) {
            if (url == null || url.isBlank() || url.length() > 500
                    || !(url.startsWith("https://") || url.startsWith("http://"))) {
                throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                        com.ticketingsystem.yuzhonblog.common.ErrorCode.BAD_REQUEST);
            }
        }
        photoWallService.batchAddPhotos(id, urls);
        return ApiResponse.success();
    }
}
