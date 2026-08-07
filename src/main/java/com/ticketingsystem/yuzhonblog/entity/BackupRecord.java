package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "backup_record")
public class BackupRecord extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
}
