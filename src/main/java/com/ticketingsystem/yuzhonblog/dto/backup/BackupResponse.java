package com.ticketingsystem.yuzhonblog.dto.backup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupResponse {
    private Long id;
    private String filename;
    private Long fileSize;
    private Integer recordCount;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
