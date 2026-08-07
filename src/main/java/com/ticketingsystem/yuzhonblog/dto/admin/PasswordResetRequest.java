package com.ticketingsystem.yuzhonblog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间")
    private String newPassword;
}
