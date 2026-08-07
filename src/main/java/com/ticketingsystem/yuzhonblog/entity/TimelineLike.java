package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "timeline_like", uniqueConstraints = @UniqueConstraint(columnNames = {"entry_id", "ip_address"}))
public class TimelineLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private TimelineEntry entry;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
}
