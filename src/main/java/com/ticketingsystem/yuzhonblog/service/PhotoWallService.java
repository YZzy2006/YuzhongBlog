package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.photowall.*;
import com.ticketingsystem.yuzhonblog.entity.Photo;
import com.ticketingsystem.yuzhonblog.entity.PhotoAlbum;
import com.ticketingsystem.yuzhonblog.repository.PhotoAlbumRepository;
import com.ticketingsystem.yuzhonblog.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoWallService {

    private final PhotoAlbumRepository photoAlbumRepository;
    private final PhotoRepository photoRepository;
    private final OssService ossService;

    // ==================== Album ====================

    @Transactional(readOnly = true)
    public PageResult<PhotoAlbumResponse> listAlbums(boolean showHidden, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<PhotoAlbum> albumPage = showHidden
                ? photoAlbumRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageRequest)
                : photoAlbumRepository.findAllByVisibleTrueOrderBySortOrderAscCreatedAtDesc(pageRequest);

        List<Long> albumIds = albumPage.getContent().stream()
                .map(PhotoAlbum::getId)
                .toList();

        // Group photos by album: count + first 3 for stacked preview
        Map<Long, Long> finalCountMap;
        Map<Long, List<PhotoResponse>> finalPreviewMap;
        if (!albumIds.isEmpty()) {
            List<Photo> allPhotos = photoRepository.findByAlbumIdIn(albumIds);
            finalCountMap = allPhotos.stream()
                    .collect(Collectors.groupingBy(Photo::getAlbumId, Collectors.counting()));
            finalPreviewMap = allPhotos.stream()
                    .collect(Collectors.groupingBy(Photo::getAlbumId))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream().limit(3).map(this::toPhotoResponse).toList()
                    ));
        } else {
            finalCountMap = Map.of();
            finalPreviewMap = Map.of();
        }

        List<PhotoAlbumResponse> responses = albumPage.getContent().stream()
                .map(album -> toAlbumResponse(album,
                        finalCountMap.getOrDefault(album.getId(), 0L),
                        finalPreviewMap.getOrDefault(album.getId(), List.of())))
                .toList();

        PageResult<PhotoAlbumResponse> result = new PageResult<>();
        result.setContent(responses);
        result.setTotalElements(albumPage.getTotalElements());
        result.setTotalPages(albumPage.getTotalPages());
        result.setNumber(albumPage.getNumber());
        result.setSize(albumPage.getSize());
        return result;
    }

    @Transactional(readOnly = true)
    public PhotoAlbumDetailResponse getAlbumDetail(Long id, boolean showHidden) {
        PhotoAlbum album = photoAlbumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

        if (!showHidden && !album.getVisible()) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }

        List<Photo> photos = photoRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(id);
        long photoCount = photos.size();

        PhotoAlbumDetailResponse response = new PhotoAlbumDetailResponse();
        response.setId(album.getId());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setCoverUrl(album.getCoverUrl());
        response.setSortOrder(album.getSortOrder());
        response.setVisible(album.getVisible());
        response.setPhotoCount(photoCount);
        response.setCreatedAt(album.getCreatedAt());
        response.setPhotos(photos.stream().map(this::toPhotoResponse).toList());
        return response;
    }

    @Transactional
    public PhotoAlbumResponse createAlbum(PhotoAlbumRequest request) {
        if (photoAlbumRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.ALBUM_NAME_DUPLICATE);
        }

        PhotoAlbum album = new PhotoAlbum();
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverUrl(request.getCoverUrl());
        album.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        album.setVisible(request.getVisible() != null ? request.getVisible() : true);

        PhotoAlbum saved = photoAlbumRepository.save(album);
        return toAlbumResponse(saved, 0L, List.of());
    }

    @Transactional
    public PhotoAlbumResponse updateAlbum(Long id, PhotoAlbumRequest request) {
        PhotoAlbum album = photoAlbumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

        if (photoAlbumRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new BusinessException(ErrorCode.ALBUM_NAME_DUPLICATE);
        }

        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverUrl(request.getCoverUrl());
        if (request.getSortOrder() != null) album.setSortOrder(request.getSortOrder());
        if (request.getVisible() != null) album.setVisible(request.getVisible());

        PhotoAlbum saved = photoAlbumRepository.save(album);
        long photoCount = photoRepository.countByAlbumId(id);
        return toAlbumResponse(saved, photoCount, List.of());
    }

    @Transactional
    public void deleteAlbum(Long id) {
        PhotoAlbum album = photoAlbumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

        // Delete OSS files for all photos in this album
        List<Photo> photos = photoRepository.findByAlbumIdOrderBySortOrderAscCreatedAtDesc(id);
        for (Photo photo : photos) {
            ossService.deleteFile(photo.getUrl());
        }

        // Delete cover from OSS
        if (album.getCoverUrl() != null && !album.getCoverUrl().isBlank()) {
            ossService.deleteFile(album.getCoverUrl());
        }

        photoRepository.deleteByAlbumId(id);
        photoAlbumRepository.delete(album);
    }

    // ==================== Photo ====================

    @Transactional
    public PhotoResponse addPhoto(PhotoRequest request) {
        if (!photoAlbumRepository.existsById(request.getAlbumId())) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }

        Photo photo = new Photo();
        photo.setAlbumId(request.getAlbumId());
        photo.setUrl(request.getUrl());
        photo.setCaption(request.getCaption());
        photo.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        Photo saved = photoRepository.save(photo);
        return toPhotoResponse(saved);
    }

    @Transactional
    public void deletePhoto(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));

        ossService.deleteFile(photo.getUrl());
        photoRepository.deleteById(id);
    }

    @Transactional
    public PhotoResponse updatePhotoCaption(Long id, String caption) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        photo.setCaption(caption);
        Photo saved = photoRepository.save(photo);
        return toPhotoResponse(saved);
    }

    @Transactional
    public void batchAddPhotos(Long albumId, List<String> urls) {
        if (!photoAlbumRepository.existsById(albumId)) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }

        List<Photo> photos = urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(url -> {
                    Photo photo = new Photo();
                    photo.setAlbumId(albumId);
                    photo.setUrl(url);
                    photo.setSortOrder(0);
                    return photo;
                })
                .toList();
        photoRepository.saveAll(photos);
    }

    // ==================== Helpers ====================

    private PhotoAlbumResponse toAlbumResponse(PhotoAlbum album, long photoCount, List<PhotoResponse> photos) {
        PhotoAlbumResponse response = new PhotoAlbumResponse();
        response.setId(album.getId());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setCoverUrl(album.getCoverUrl());
        response.setSortOrder(album.getSortOrder());
        response.setVisible(album.getVisible());
        response.setPhotoCount(photoCount);
        response.setPhotos(photos);
        response.setCreatedAt(album.getCreatedAt());
        return response;
    }

    private PhotoResponse toPhotoResponse(Photo photo) {
        PhotoResponse response = new PhotoResponse();
        response.setId(photo.getId());
        response.setAlbumId(photo.getAlbumId());
        response.setUrl(photo.getUrl());
        response.setCaption(photo.getCaption());
        response.setSortOrder(photo.getSortOrder());
        response.setCreatedAt(photo.getCreatedAt());
        return response;
    }
}
