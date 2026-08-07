package com.ticketingsystem.yuzhonblog.dto.photowall;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoResponse {

    private Long id;
    private Long albumId;
    private String url;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
