package com.ticketingsystem.yuzhonblog.dto.announcement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnnouncementCreateRequest {

    @NotBlank(message = "标签不能为空")
    @Size(max = 20)
    private String tag;

    @Size(max = 30)
    private String tagEn;

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "^(info|feature|update)$", message = "类型必须是 info、feature 或 update")
    private String type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String titleEn;

    @Size(max = 5000)
    @Pattern(regexp = "^[^<>]*$", message = "内容不能包含HTML标签")
    private String content;

    @Size(max = 5000)
    @Pattern(regexp = "^[^<>]*$", message = "内容不能包含HTML标签")
    private String contentEn;

    @Min(0)
    @Max(9999)
    private Integer sortOrder;

    @Pattern(regexp = "^(info|success|warning|error)$", message = "颜色等级必须是 info、success、warning 或 error")
    private String level;

    @Pattern(regexp = "^(banner|alert)$", message = "展示样式必须是 banner 或 alert")
    private String displayStyle;
}
