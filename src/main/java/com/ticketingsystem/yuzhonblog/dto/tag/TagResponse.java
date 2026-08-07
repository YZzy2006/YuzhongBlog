package com.ticketingsystem.yuzhonblog.dto.tag;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagResponse {
    private Long id;
    private String name;
    private Long articleCount;
    private LocalDateTime createdAt;
}
