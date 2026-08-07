package com.ticketingsystem.yuzhonblog.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String name;
    private String avatarUrl;
    private String role;
    private List<String> permissions;

    public LoginResponse(String accessToken, String refreshToken, String username, String name, String avatarUrl, String role, List<String> permissions) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.permissions = permissions;
    }
}
