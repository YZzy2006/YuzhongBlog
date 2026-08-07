package com.ticketingsystem.yuzhonblog.dto.friendlink;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class FriendLinkRequest {

    @NotBlank(message = "友链名称不能为空")
    @Size(max = 100, message = "友链名称不能超过100字")
    private String name;

    @NotBlank(message = "友链地址不能为空")
    @URL(message = "友链地址格式不正确")
    @Size(max = 500, message = "友链地址不能超过500字")
    private String url;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    @URL(message = "头像地址格式不正确")
    @Size(max = 500, message = "头像地址不能超过500字")
    private String avatar;

    @Size(max = 50, message = "主题色不能超过50字")
    private String themeColor;

    private Integer sortOrder;
}
