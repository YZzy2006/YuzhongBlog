package com.ticketingsystem.yuzhonblog.dto.announcement;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementResponse {
    private Long id;
    private String tag;
    private String tagEn;
    private String type;
    private String title;
    private String titleEn;
    private String content;
    private String contentEn;
    private Integer sortOrder;
    private Boolean active;
    private String level;
    private String displayStyle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
