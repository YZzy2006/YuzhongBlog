package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.AiConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AiConfigRepository extends JpaRepository<AiConfigEntity, Long> {
    @Query("SELECT a FROM AiConfigEntity a WHERE a.isActive = true")
    Optional<AiConfigEntity> findFirstActive();
    List<AiConfigEntity> findAllByOrderBySortOrderDesc();

    @Modifying
    @Query("UPDATE AiConfigEntity a SET a.isActive = false WHERE a.isActive = true")
    int deactivateAll();
}
