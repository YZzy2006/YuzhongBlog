package com.ticketingsystem.yuzhonblog.dto.admin;

import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileResponse {
    private Long id;
    private String username;
    private String name;
    private String realName;
    private String bio;
    private String avatarUrl;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;

    public static ProfileResponse from(AdminUser user) {
        ProfileResponse r = new ProfileResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.name = user.getName();
        r.realName = user.getRealName();
        r.bio = user.getBio();
        r.avatarUrl = user.getAvatarUrl();
        r.email = user.getEmail();
        r.phone = user.getPhone();
        r.role = user.getRole();
        r.createdAt = user.getCreatedAt();
        return r;
    }
}
