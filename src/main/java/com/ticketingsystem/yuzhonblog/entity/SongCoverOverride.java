package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "song_cover_override", uniqueConstraints = {
    @UniqueConstraint(columnNames = "song_id")
})
public class SongCoverOverride extends BaseEntity {

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "custom_cover_url", nullable = false, length = 500)
    private String customCoverUrl;

    @Column(name = "custom_name", length = 200)
    private String customName;

    @Column(name = "custom_artist", length = 200)
    private String customArtist;
}
