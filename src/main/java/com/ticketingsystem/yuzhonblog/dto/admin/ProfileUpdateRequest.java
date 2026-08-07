package com.ticketingsystem.yuzhonblog.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 50, message = "姓名不能超过50字")
    private String name;
    @Size(max = 100, message = "真实姓名不能超过100字")
    private String realName;
    @Size(max = 500, message = "简介不能超过500字")
    private String bio;
    @Size(max = 100, message = "邮箱不能超过100字")
    private String email;
}
