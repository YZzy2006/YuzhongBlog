package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.admin.ProfileResponse;
import com.ticketingsystem.yuzhonblog.dto.admin.ProfileUpdateRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.service.OssService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUserProvider currentUserProvider;
    private final AdminUserRepository adminUserRepository;
    private final OssService ossService;

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile() {
        AdminUser user = currentUserProvider.getCurrentUser();
        return ApiResponse.success(ProfileResponse.from(user));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        AdminUser user = currentUserProvider.getCurrentUser();
        if (request.getName() != null) user.setName(request.getName());
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        adminUserRepository.save(user);
        return ApiResponse.success(ProfileResponse.from(user));
    }

    @PutMapping("/avatar")
    public ApiResponse<ProfileResponse> updateAvatar(@RequestBody java.util.Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String lower = avatarUrl.toLowerCase();
            if (!lower.startsWith("https://") ||
                    lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
                throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                        com.ticketingsystem.yuzhonblog.common.ErrorCode.BAD_REQUEST);
            }
        }
        AdminUser user = currentUserProvider.getCurrentUser();
        user.setAvatarUrl(avatarUrl);
        adminUserRepository.save(user);
        return ApiResponse.success(ProfileResponse.from(user));
    }

    @PostMapping("/avatar")
    public ApiResponse<ProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        AdminUser user = currentUserProvider.getCurrentUser();
        String url = ossService.uploadFile(file, "avatar");
        user.setAvatarUrl(url);
        adminUserRepository.save(user);
        return ApiResponse.success(ProfileResponse.from(user));
    }
}
