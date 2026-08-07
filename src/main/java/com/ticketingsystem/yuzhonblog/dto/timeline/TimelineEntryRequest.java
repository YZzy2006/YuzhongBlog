package com.ticketingsystem.yuzhonblog.dto.timeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
public class TimelineEntryRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 5000, message = "描述不能超过5000字")
    private String description;

    @Size(max = 500, message = "封面图URL不能超过500字")
    @URL(message = "封面图URL格式不正确")
    private String coverImage;

    @NotNull(message = "日期不能为空")
    private LocalDate entryDate;

    @Size(max = 500, message = "链接URL不能超过500字")
    @URL(message = "链接URL格式不正确")
    private String linkUrl;

    @Size(max = 50, message = "分类不能超过50字")
    private String category;

    private Integer sortOrder = 0;

    @Pattern(regexp = "^(DRAFT|PUBLISHED)$", message = "状态只能是 DRAFT 或 PUBLISHED")
    private String status;

    @Size(max = 20, message = "心情不能超过20字")
    private String mood;

    private String tags;

    private String images;
}
