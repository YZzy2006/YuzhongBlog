package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.ContentReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentReviewRepository extends JpaRepository<ContentReview, Long> {

    Page<ContentReview> findByReviewStatusOrderByCreatedAtDesc(String reviewStatus, Pageable pageable);

    Page<ContentReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<ContentReview> findTopByContentTypeAndContentIdAndReviewStatusOrderByCreatedAtDesc(
            String contentType, Long contentId, String reviewStatus);

    void deleteByContentTypeAndContentId(String contentType, Long contentId);
}
