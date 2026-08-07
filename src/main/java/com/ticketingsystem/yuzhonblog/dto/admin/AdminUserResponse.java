package com.ticketingsystem.yuzhonblog.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(AdminUser user) {
        AdminUserResponse resp = new AdminUserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setEnabled(user.getEnabled());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }
}
