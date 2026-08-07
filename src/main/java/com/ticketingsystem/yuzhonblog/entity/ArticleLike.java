package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@BatchSize(size = 20)
@Table(name = "article_like", uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "ip_address"}))
public class ArticleLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
}
