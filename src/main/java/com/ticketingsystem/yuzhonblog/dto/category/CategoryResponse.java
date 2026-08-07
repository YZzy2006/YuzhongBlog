package com.ticketingsystem.yuzhonblog.dto.category;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private Integer sortOrder;
    private Long articleCount;  // 该分类下文章数量
    private LocalDateTime createdAt;
}
