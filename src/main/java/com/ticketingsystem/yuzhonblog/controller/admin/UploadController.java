package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssService ossService;

    @PostMapping("/image")
    @RequirePermission("upload:file")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = ossService.uploadFile(file, "content");
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping("/cover")
    @RequirePermission("upload:file")
    public ApiResponse<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = ossService.uploadFile(file, "cover");
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping("/document")
    @RequirePermission("upload:file")
    public ApiResponse<Map<String, String>> uploadDocument(@RequestParam("file") MultipartFile file) {
        String url = ossService.uploadDocument(file, "document");
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping("/music-cover")
    @RequirePermission("upload:file")
    public ApiResponse<Map<String, String>> uploadMusicCover(@RequestParam("file") MultipartFile file) {
        String url = ossService.uploadFile(file, "music-cover");
        return ApiResponse.success(Map.of("url", url));
    }

    @DeleteMapping("/image")
    @RequirePermission("upload:file")
    public ApiResponse<Void> deleteImage(@RequestParam String url) {
        ossService.deleteFile(url);
        return ApiResponse.success();
    }
}
