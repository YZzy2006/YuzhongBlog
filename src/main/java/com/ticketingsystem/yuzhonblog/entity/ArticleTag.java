package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@BatchSize(size = 50)
@Table(name = "article_tag",
       indexes = @Index(name = "idx_article_tag_tag_id", columnList = "tag_id"),
       uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "tag_id"}))
public class ArticleTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
