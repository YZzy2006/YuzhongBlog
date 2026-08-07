package com.ticketingsystem.yuzhonblog.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @Size(max = 100, message = "真实姓名不能超过100字")
    private String realName;

    @Size(max = 100, message = "邮箱不能超过100字")
    private String email;

    private String role;
}
