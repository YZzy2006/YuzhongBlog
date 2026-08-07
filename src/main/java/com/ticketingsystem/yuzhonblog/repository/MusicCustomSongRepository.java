package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.MusicCustomSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicCustomSongRepository extends JpaRepository<MusicCustomSong, Long> {
    List<MusicCustomSong> findAllByOrderBySortOrderAsc();
    boolean existsBySourceId(String sourceId);
    Optional<MusicCustomSong> findBySourceId(String sourceId);
}
