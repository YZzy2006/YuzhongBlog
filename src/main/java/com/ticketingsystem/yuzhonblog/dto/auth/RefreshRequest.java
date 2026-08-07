package com.ticketingsystem.yuzhonblog.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank(message = "refreshToken不能为空")
    @Size(max = 2000, message = "refreshToken过长")
    private String refreshToken;
}
