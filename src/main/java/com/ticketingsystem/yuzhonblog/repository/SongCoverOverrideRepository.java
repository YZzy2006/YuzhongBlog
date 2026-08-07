package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.SongCoverOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SongCoverOverrideRepository extends JpaRepository<SongCoverOverride, Long> {
    Optional<SongCoverOverride> findBySongId(Long songId);
    List<SongCoverOverride> findBySongIdIn(List<Long> songIds);
}
