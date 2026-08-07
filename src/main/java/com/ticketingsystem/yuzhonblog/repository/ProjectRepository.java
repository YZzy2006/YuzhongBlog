package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderBySortOrderDescCreatedAtDesc();

    List<Project> findByStatusOrderBySortOrderDescCreatedAtDesc(String status);

    @Query("SELECT p FROM Project p WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.subtitle LIKE CONCAT('%', :keyword, '%')) AND (:status IS NULL OR p.status = :status)")
    Page<Project> findAdminFiltered(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.status = 'PUBLISHED' " +
           "AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.subtitle LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%') OR p.techStack LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:techStack IS NULL OR p.techStack LIKE CONCAT('%', :techStack, '%')) " +
           "AND (:featured IS NULL OR p.isFeatured = :featured) " +
           "AND (:dateFrom IS NULL OR p.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.createdAt <= :dateTo) " +
           "ORDER BY p.sortOrder DESC, p.createdAt DESC")
    List<Project> searchPublic(@Param("keyword") String keyword,
                               @Param("techStack") String techStack,
                               @Param("featured") Boolean featured,
                               @Param("dateFrom") LocalDateTime dateFrom,
                               @Param("dateTo") LocalDateTime dateTo);
}
