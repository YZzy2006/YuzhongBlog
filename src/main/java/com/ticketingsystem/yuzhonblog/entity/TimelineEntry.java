package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "timeline_entry", indexes = {
    @Index(name = "idx_timeline_status", columnList = "status"),
    @Index(name = "idx_timeline_entry_date", columnList = "entry_date")
})
public class TimelineEntry extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(length = 50)
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(length = 20)
    private String status = "DRAFT";

    @Column(length = 20)
    private String mood;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;
}
