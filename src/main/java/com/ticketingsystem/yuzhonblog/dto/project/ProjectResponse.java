package com.ticketingsystem.yuzhonblog.dto.project;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String techStack;
    private String coverImage;
    private String githubUrl;
    private String demoUrl;
    private Integer sortOrder;
    private String subtitle;
    private String features;
    private String subdomainUrl;
    private String screenshots;
    private String status;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}