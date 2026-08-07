package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "photo", indexes = @Index(name = "idx_photo_album_id", columnList = "album_id"))
public class Photo extends BaseEntity {

    @Column(name = "album_id", nullable = false)
    private Long albumId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 200)
    private String caption;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
