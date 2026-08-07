package com.ticketingsystem.yuzhonblog.dto.photowall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhotoAlbumRequest {

    @NotBlank(message = "相册名称不能为空")
    @Size(max = 100, message = "相册名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "相册描述不能超过500个字符")
    private String description;

    @Size(max = 500, message = "封面URL不能超过500个字符")
    private String coverUrl;

    private Integer sortOrder = 0;

    private Boolean visible = true;
}
