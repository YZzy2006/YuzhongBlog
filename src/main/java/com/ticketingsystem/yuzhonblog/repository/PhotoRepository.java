package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByAlbumIdOrderBySortOrderAscCreatedAtDesc(Long albumId);

    long countByAlbumId(Long albumId);

    void deleteByAlbumId(Long albumId);

    List<Photo> findByAlbumIdIn(List<Long> albumIds);
}
