package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.friendlink.FriendLinkRequest;
import com.ticketingsystem.yuzhonblog.dto.friendlink.FriendLinkResponse;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.FriendLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/friend-links")
@RequiredArgsConstructor
public class FriendLinkAdminController {

    private final FriendLinkService friendLinkService;

    @GetMapping
    @RequirePermission("project:view")
    public ApiResponse<List<FriendLinkResponse>> list() {
        return ApiResponse.success(friendLinkService.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("project:view")
    public ApiResponse<FriendLinkResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(friendLinkService.getById(id));
    }

    @PostMapping
    @RequirePermission("project:manage")
    public ApiResponse<FriendLinkResponse> create(@Valid @RequestBody FriendLinkRequest request) {
        return ApiResponse.success(friendLinkService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<FriendLinkResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody FriendLinkRequest request) {
        return ApiResponse.success(friendLinkService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        friendLinkService.delete(id);
        return ApiResponse.success();
    }
}
