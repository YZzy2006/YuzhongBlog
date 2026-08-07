package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.WeatherConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WeatherConfigRepository extends JpaRepository<WeatherConfigEntity, Long> {

    List<WeatherConfigEntity> findAllByOrderByIdDesc();

    Optional<WeatherConfigEntity> findFirstByIsActiveTrue();

    @Modifying
    @Query("UPDATE WeatherConfigEntity w SET w.isActive = false WHERE w.isActive = true")
    int deactivateAll();
}
