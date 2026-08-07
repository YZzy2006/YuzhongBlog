package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();

    List<Announcement> findAllByOrderBySortOrderAscCreatedAtDesc();

    @Query("SELECT a FROM Announcement a WHERE a.active = true " +
           "AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.tag LIKE %:keyword% OR a.content LIKE %:keyword%) " +
           "AND (:type IS NULL OR a.type = :type) " +
           "AND (:tag IS NULL OR a.tag = :tag)")
    Page<Announcement> findPublicFiltered(@Param("keyword") String keyword,
                                          @Param("type") String type,
                                          @Param("tag") String tag,
                                          Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND (a.titleEn IS NULL OR a.titleEn = '' OR a.contentEn IS NULL OR a.contentEn = '')")
    List<Announcement> findNeedingTranslation();

    @Query("SELECT a FROM Announcement a WHERE " +
           "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.tag LIKE %:keyword% OR a.content LIKE %:keyword%) " +
           "AND (:type IS NULL OR a.type = :type) " +
           "AND (:active IS NULL OR a.active = :active)")
    Page<Announcement> findAdminFiltered(@Param("keyword") String keyword,
                                         @Param("type") String type,
                                         @Param("active") Boolean active,
                                         Pageable pageable);
}
