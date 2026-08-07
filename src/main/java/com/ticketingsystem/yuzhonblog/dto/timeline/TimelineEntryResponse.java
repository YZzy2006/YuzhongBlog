package com.ticketingsystem.yuzhonblog.dto.timeline;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimelineEntryResponse {
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private LocalDate entryDate;
    private String linkUrl;
    private String category;
    private Integer sortOrder;
    private String status;
    private String mood;
    private String tags;
    private String images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer likeCount;
}
