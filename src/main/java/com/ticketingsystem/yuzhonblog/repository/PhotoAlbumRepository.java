package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.PhotoAlbum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoAlbumRepository extends JpaRepository<PhotoAlbum, Long> {

    Page<PhotoAlbum> findAllByVisibleTrueOrderBySortOrderAscCreatedAtDesc(Pageable pageable);

    Page<PhotoAlbum> findAllByOrderBySortOrderAscCreatedAtDesc(Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
