package com.ticketingsystem.yuzhonblog.dto.article;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleListItem {
    private Long id;
    private String title;
    private String titleEn;
    private String summary;
    private String summaryEn;
    private String coverImage;
    private String slug;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer isTop;
    private Integer isFeatured;
    private Integer cardStyle;
    private Long categoryId;
    private String categoryName;
    private List<ArticleResponse.TagInfo> tags;
    private LocalDateTime createdAt;
}
