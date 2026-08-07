package com.ticketingsystem.yuzhonblog.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50字")
    private String name;
}
