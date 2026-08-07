package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.TimelineLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimelineLikeRepository extends JpaRepository<TimelineLike, Long> {

    Optional<TimelineLike> findByEntryIdAndIpAddress(Long entryId, String ipAddress);

    boolean existsByEntryIdAndIpAddress(Long entryId, String ipAddress);
}
