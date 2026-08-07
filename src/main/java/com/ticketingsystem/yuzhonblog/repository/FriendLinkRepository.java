package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.FriendLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {
    List<FriendLink> findAllByOrderBySortOrderAscCreatedAtDesc();
    boolean existsByUrl(String url);
}
