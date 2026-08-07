package com.ticketingsystem.yuzhonblog.dto.article;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleDetailResponse extends ArticleResponse {

    private NavItem prevArticle;
    private NavItem nextArticle;
    private List<RelatedArticle> relatedArticles;

    @Data
    public static class NavItem {
        private Long id;
        private String title;
        private String titleEn;
        private String slug;

        public NavItem(Long id, String title, String titleEn, String slug) {
            this.id = id;
            this.title = title;
            this.titleEn = titleEn;
            this.slug = slug;
        }
    }

    @Data
    public static class RelatedArticle {
        private Long id;
        private String title;
        private String titleEn;
        private String summary;
        private String summaryEn;
        private String slug;
        private String categoryName;
        private Integer viewCount;
        private Integer likeCount;
    }
}
