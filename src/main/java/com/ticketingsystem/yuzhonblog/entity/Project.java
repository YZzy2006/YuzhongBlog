package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "project", indexes = {
    @Index(name = "idx_project_status", columnList = "status")
})
public class Project extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tech_stack", length = 500)
    private String techStack;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "demo_url", length = 500)
    private String demoUrl;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(length = 200)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "subdomain_url", length = 500)
    private String subdomainUrl;

    @Column(columnDefinition = "TEXT")
    private String screenshots;

    @Column(length = 20)
    private String status = "DRAFT";

    @Column(name = "is_featured")
    private Boolean isFeatured = false;
}
