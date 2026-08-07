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
@Table(name = "category")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
