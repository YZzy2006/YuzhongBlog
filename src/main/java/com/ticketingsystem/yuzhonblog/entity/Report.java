package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "report", indexes = {
    @Index(name = "idx_report_type_created", columnList = "report_type, created_at")
})
public class Report extends BaseEntity {

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;  // daily, weekly, monthly

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "data_snapshot", columnDefinition = "TEXT")
    private String dataSnapshot;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
