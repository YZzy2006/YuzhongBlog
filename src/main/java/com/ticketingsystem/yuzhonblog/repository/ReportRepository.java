package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r WHERE (:type IS NULL OR r.reportType = :type) AND (:keyword IS NULL OR r.title LIKE %:keyword% OR r.content LIKE %:keyword%) ORDER BY r.createdAt DESC")
    Page<Report> search(@Param("type") String type, @Param("keyword") String keyword, Pageable pageable);
}
