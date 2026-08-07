package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "announcement", indexes = {
    @Index(name = "idx_announcement_active_sort", columnList = "active, sort_order, created_at")
})
public class Announcement extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String tag;

    @Column(name = "tag_en", length = 30)
    private String tagEn;

    @Column(nullable = false, length = 20)
    private String type; // info, feature, update

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 20)
    private String level = "info";   // info / success / warning / error

    @Column(name = "display_style", nullable = false, length = 20)
    private String displayStyle = "banner";  // banner / alert

    @Column(nullable = false)
    private Boolean active = true;
}
