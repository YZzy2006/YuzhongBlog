package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "friend_link")
public class FriendLink extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String avatar;

    @Column(name = "theme_color", length = 50)
    private String themeColor;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
