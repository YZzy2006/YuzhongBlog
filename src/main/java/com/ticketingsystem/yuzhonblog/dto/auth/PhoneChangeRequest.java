package com.ticketingsystem.yuzhonblog.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhoneChangeRequest {
    @NotBlank(message = "新手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String newPhone;

    @NotBlank(message = "解锁密码不能为空")
    @Size(min = 4, max = 20, message = "解锁密码长度为4-20位")
    private String unlockPassword;
}
