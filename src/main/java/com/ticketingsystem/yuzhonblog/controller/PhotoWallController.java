package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.photowall.PhotoAlbumDetailResponse;
import com.ticketingsystem.yuzhonblog.dto.photowall.PhotoAlbumResponse;
import com.ticketingsystem.yuzhonblog.service.PhotoWallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/photowall")
@RequiredArgsConstructor
public class PhotoWallController {

    private final PhotoWallService photoWallService;

    @GetMapping("/albums")
    public ApiResponse<PageResult<PhotoAlbumResponse>> listAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ApiResponse.success(photoWallService.listAlbums(false, page, size));
    }

    @GetMapping("/albums/{id}")
    public ApiResponse<PhotoAlbumDetailResponse> getAlbumDetail(@PathVariable Long id) {
        return ApiResponse.success(photoWallService.getAlbumDetail(id, false));
    }
}
