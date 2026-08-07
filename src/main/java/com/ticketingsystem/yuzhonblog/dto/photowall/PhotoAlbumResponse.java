package com.ticketingsystem.yuzhonblog.dto.photowall;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PhotoAlbumResponse {

    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Integer sortOrder;
    private Boolean visible;
    private Long photoCount;
    private List<PhotoResponse> photos;
    private LocalDateTime createdAt;
}
