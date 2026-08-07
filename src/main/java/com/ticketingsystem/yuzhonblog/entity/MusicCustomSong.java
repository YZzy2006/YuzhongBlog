package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "music_custom_song", uniqueConstraints = {
    @UniqueConstraint(columnNames = "source_id")
}, indexes = {
    @Index(name = "idx_mcs_source_id", columnList = "source_id")
})
public class MusicCustomSong extends BaseEntity {

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType; // "bilibili"

    @Column(name = "source_id", nullable = false, length = 50)
    private String sourceId; // BVID

    @Column(length = 200)
    private String title;

    @Column(length = 200)
    private String artist;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "custom_cover_url", length = 500)
    private String customCoverUrl;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "duration")
    private Integer duration = 0; // seconds
}
