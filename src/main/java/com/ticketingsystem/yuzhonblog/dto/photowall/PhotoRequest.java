package com.ticketingsystem.yuzhonblog.dto.photowall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhotoRequest {

    @NotNull(message = "相册ID不能为空")
    private Long albumId;

    @NotBlank(message = "图片URL不能为空")
    @Size(max = 500, message = "图片URL不能超过500个字符")
    private String url;

    @Size(max = 200, message = "图片说明不能超过200个字符")
    private String caption;

    private Integer sortOrder = 0;
}
