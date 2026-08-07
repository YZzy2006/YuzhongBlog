package com.ticketingsystem.yuzhonblog.dto.article;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String titleEn;
    private String contentMd;
    private String contentMdEn;
    private String contentHtml;
    private String contentHtmlEn;
    private String summary;
    private String summaryEn;
    private String coverImage;
    private String slug;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean liked;
    private Integer isTop;
    private Integer isFeatured;
    private Integer cardStyle;
    private String authorNotes;
    private Long categoryId;
    private String categoryName;
    private List<TagInfo> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class TagInfo {
        private Long id;
        private String name;
    }
}
