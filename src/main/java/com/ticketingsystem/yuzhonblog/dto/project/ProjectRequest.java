package com.ticketingsystem.yuzhonblog.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过100字")
    private String name;

    @Size(max = 50000, message = "描述不能超过50000字")
    private String description;
    @Size(max = 500, message = "技术栈不能超过500字")
    private String techStack;
    @Size(max = 500, message = "封面图URL不能超过500字")
    private String coverImage;
    @Size(max = 500, message = "GitHub URL不能超过500字")
    private String githubUrl;
    @Size(max = 500, message = "Demo URL不能超过500字")
    private String demoUrl;
    private Integer sortOrder = 0;
    @Size(max = 200, message = "副标题不能超过200字")
    private String subtitle;
    @Size(max = 50000, message = "功能特性不能超过50000字")
    private String features;
    @Size(max = 500, message = "子域名URL不能超过500字")
    private String subdomainUrl;
    @Size(max = 20000, message = "展示图片列表过长")
    private String screenshots;
    @Size(max = 50, message = "状态值不能超过50字")
    private String status;
    private Boolean isFeatured;
}