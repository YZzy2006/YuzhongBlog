package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.TimelineEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, Long> {

    List<TimelineEntry> findByStatusOrderByEntryDateDescSortOrderDesc(String status);

    List<TimelineEntry> findAllByOrderByEntryDateDescSortOrderDesc();

    @Query("SELECT t FROM TimelineEntry t WHERE (:keyword IS NULL OR t.title LIKE CONCAT('%', :keyword, '%') OR t.description LIKE CONCAT('%', :keyword, '%')) AND (:status IS NULL OR t.status = :status)")
    Page<TimelineEntry> findAdminFiltered(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Modifying
    @Query("UPDATE TimelineEntry e SET e.viewCount = COALESCE(e.viewCount, 0) + 1 WHERE e.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TimelineEntry e SET e.likeCount = COALESCE(e.likeCount, 0) + 1 WHERE e.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TimelineEntry e SET e.likeCount = GREATEST(COALESCE(e.likeCount, 0) - 1, 0) WHERE e.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Query("SELECT e.likeCount FROM TimelineEntry e WHERE e.id = :id")
    java.util.Optional<Integer> findLikeCountById(@Param("id") Long id);
}
