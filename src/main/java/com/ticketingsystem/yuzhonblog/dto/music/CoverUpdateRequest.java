package com.ticketingsystem.yuzhonblog.dto.music;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CoverUpdateRequest {
    @NotBlank(message = "封面URL不能为空")
    @Size(max = 500, message = "封面URL不能超过500字")
    private String coverUrl;
}
