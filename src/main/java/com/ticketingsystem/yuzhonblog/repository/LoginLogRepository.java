package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    @Query("SELECT l FROM LoginLog l WHERE " +
           "(:keyword IS NULL OR l.username LIKE %:keyword% OR l.loginIp LIKE %:keyword% OR l.location LIKE %:keyword%) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:start IS NULL OR l.createdAt >= :start) AND " +
           "(:end IS NULL OR l.createdAt <= :end) " +
           "ORDER BY l.createdAt DESC")
    Page<LoginLog> search(@Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          Pageable pageable);

    @Query("SELECT l FROM LoginLog l WHERE " +
           "(:keyword IS NULL OR l.username LIKE %:keyword% OR l.loginIp LIKE %:keyword% OR l.location LIKE %:keyword%) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:start IS NULL OR l.createdAt >= :start) AND " +
           "(:end IS NULL OR l.createdAt <= :end) " +
           "ORDER BY l.createdAt DESC")
    List<LoginLog> searchAll(@Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             Pageable pageable);

    @Query("SELECT FUNCTION('DATE', l.createdAt) as day, COUNT(l) as cnt FROM LoginLog l " +
           "WHERE l.userId = :userId AND l.status = 1 AND l.createdAt >= :since " +
           "GROUP BY FUNCTION('DATE', l.createdAt) ORDER BY day")
    List<Object[]> countSuccessfulLoginsByDay(@Param("userId") Long userId,
                                               @Param("since") LocalDateTime since);

    @Query("SELECT l.username, COUNT(l) as failCount, MAX(l.createdAt) as lastFail FROM LoginLog l " +
           "WHERE l.status = 0 AND l.createdAt >= :since " +
           "GROUP BY l.username HAVING COUNT(l) >= :threshold ORDER BY failCount DESC")
    List<Object[]> findUsersWithExcessiveFailures(@Param("since") LocalDateTime since,
                                                   @Param("threshold") long threshold);
}
