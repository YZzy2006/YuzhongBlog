package com.ticketingsystem.yuzhonblog.dto.article;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ArticleCreateRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 200, message = "英文标题不能超过200字")
    private String titleEn;

    @Size(max = 50000, message = "Markdown内容不能超过50000字")
    private String contentMd;
    @Size(max = 50000, message = "英文Markdown内容不能超过50000字")
    private String contentMdEn;
    @Size(max = 100000, message = "HTML内容不能超过100000字")
    private String contentHtml;
    @Size(max = 100000, message = "英文HTML内容不能超过100000字")
    private String contentHtmlEn;

    @Size(max = 500, message = "摘要不能超过500字")
    private String summary;

    @Size(max = 500, message = "英文摘要不能超过500字")
    private String summaryEn;

    @Size(max = 500, message = "封面图URL不能超过500字")
    private String coverImage;
    @Size(max = 200, message = "Slug不能超过200字")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug只能包含小写字母、数字和连字符")
    private String slug;
    private Long categoryId;
    @Size(max = 50, message = "标签数量不能超过50")
    private List<Long> tagIds;
    @Min(value = 0, message = "卡片样式不能小于0")
    @Max(value = 39, message = "卡片样式不能大于39")
    private Integer cardStyle;
    @Size(max = 2000, message = "作者的话不能超过2000字")
    private String authorNotes;
}
