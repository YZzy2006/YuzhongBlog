package com.ticketingsystem.yuzhonblog.dto.friendlink;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendLinkResponse {
    private Long id;
    private String name;
    private String url;
    private String description;
    private String avatar;
    private String themeColor;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
