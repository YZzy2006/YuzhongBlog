package com.ticketingsystem.yuzhonblog.dto.video;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoResponse {
    private String bvid;
    private String title;
    private String cover;
    private String author;
    private Integer duration;
}
