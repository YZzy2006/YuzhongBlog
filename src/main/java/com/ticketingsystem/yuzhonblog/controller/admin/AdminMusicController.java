package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.music.CoverUpdateRequest;
import com.ticketingsystem.yuzhonblog.dto.music.MusicSongResponse;
import com.ticketingsystem.yuzhonblog.entity.MusicCustomSong;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.MusicCustomSongService;
import com.ticketingsystem.yuzhonblog.service.MusicService;
import com.ticketingsystem.yuzhonblog.service.SiteSettingService;
import com.ticketingsystem.yuzhonblog.service.SongCoverOverrideService;
import com.ticketingsystem.yuzhonblog.service.VideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/music")
@RequiredArgsConstructor
public class AdminMusicController {

    private final MusicService musicService;
    private final SiteSettingService siteSettingService;
    private final MusicCustomSongService customSongService;
    private final SongCoverOverrideService coverOverrideService;
    private final VideoService videoService;

    // ==================== NetEase Songs ====================

    @GetMapping("/songs")
    @RequirePermission("setting:edit")
    public ApiResponse<List<MusicSongResponse>> list() {
        List<Long> ids = getMusicIds();
        if (ids.isEmpty()) return ApiResponse.success(Collections.emptyList());
        return ApiResponse.success(musicService.getSongsByIds(ids));
    }

    @GetMapping("/songs/query/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> query(@PathVariable Long id) {
        try {
            List<MusicSongResponse> result = musicService.getSongsByIds(List.of(id));
            if (!result.isEmpty()) {
                return ApiResponse.success(Map.of("type", "song", "data", result.get(0)));
            }

            Map<String, Object> albumInfo = musicService.getAlbumInfo(id);
            if (albumInfo != null) {
                List<Long> songIds = (List<Long>) albumInfo.get("songIds");
                List<MusicSongResponse> songs = musicService.getSongsByIds(songIds);
                Map<String, Object> albumResult = new LinkedHashMap<>();
                albumResult.put("type", "album");
                albumResult.put("albumName", albumInfo.get("albumName"));
                albumResult.put("artistName", albumInfo.get("artistName"));
                albumResult.put("coverUrl", albumInfo.get("coverUrl"));
                albumResult.put("count", songs.size());
                albumResult.put("songs", songs);
                return ApiResponse.success(albumResult);
            }

            return ApiResponse.error(404, "NOT_FOUND");
        } catch (Exception e) {
            return ApiResponse.error(500, "QUERY_FAILED");
        }
    }

    @PostMapping("/songs")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> add(@RequestParam Long id) {
        List<Long> ids = getMusicIds();
        if (!ids.contains(id)) {
            ids.add(id);
            saveMusicIds(ids);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/songs/batch")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> addBatch(@RequestBody @Valid @Size(max = 500, message = "单次最多500个") List<Long> newIds) {
        List<Long> ids = getMusicIds();
        for (Long id : newIds) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        saveMusicIds(ids);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/songs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        List<Long> ids = getMusicIds();
        ids.remove(id);
        saveMusicIds(ids);
        return ApiResponse.success(null);
    }

    @PutMapping("/songs/reorder")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> reorder(@RequestBody @Valid @Size(max = 500, message = "单次最多500个") List<Long> orderedIds) {
        siteSettingService.setSetting("music_ids",
                orderedIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return ApiResponse.success(null);
    }

    // ==================== NetEase Song Cover Override ====================

    @PutMapping("/songs/{songId}/cover")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> updateNeteaseCover(@PathVariable Long songId,
                                                 @Valid @RequestBody CoverUpdateRequest request) {
        coverOverrideService.setOverride(songId, request.getCoverUrl());
        return ApiResponse.success();
    }

    @PutMapping("/songs/{songId}/meta")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> updateNeteaseMeta(@PathVariable Long songId,
                                                @RequestBody SongMetaRequest request) {
        coverOverrideService.setMetaOverride(songId, request.getName(), request.getArtist());
        return ApiResponse.success();
    }

    @DeleteMapping("/songs/{songId}/cover")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> removeNeteaseCover(@PathVariable Long songId) {
        coverOverrideService.removeOverride(songId);
        return ApiResponse.success();
    }

    // ==================== Bilibili Songs ====================

    @GetMapping("/bilibili-songs")
    @RequirePermission("setting:edit")
    public ApiResponse<List<MusicCustomSong>> listBilibiliSongs() {
        return ApiResponse.success(customSongService.listAll());
    }

    @PostMapping("/bilibili-songs")
    @RequirePermission("setting:edit")
    public ApiResponse<MusicCustomSong> addBilibiliSong(@RequestParam String bvid) {
        return ApiResponse.success(customSongService.add(bvid));
    }

    @DeleteMapping("/bilibili-songs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> removeBilibiliSong(@PathVariable Long id) {
        customSongService.remove(id);
        return ApiResponse.success();
    }

    @PutMapping("/bilibili-songs/reorder")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> reorderBilibiliSongs(@RequestBody @Valid @Size(max = 500) List<Long> orderedIds) {
        customSongService.reorder(orderedIds);
        return ApiResponse.success();
    }

    @GetMapping("/bilibili-songs/query/{bvid}")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, String>> queryBilibili(@PathVariable String bvid) {
        return ApiResponse.success(customSongService.queryBilibili(bvid));
    }

    @PutMapping("/bilibili-songs/{id}/cover")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> updateBilibiliCover(@PathVariable Long id,
                                                  @Valid @RequestBody CoverUpdateRequest request) {
        customSongService.updateCover(id, request.getCoverUrl());
        return ApiResponse.success();
    }

    @PutMapping("/bilibili-songs/{id}/meta")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> updateBilibiliMeta(@PathVariable Long id,
                                                 @RequestBody SongMetaRequest request) {
        customSongService.updateMeta(id, request.getName(), request.getArtist());
        return ApiResponse.success();
    }

    @PostMapping("/bilibili-songs/refresh")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> refreshBilibiliMeta() {
        int updated = customSongService.refreshAllMeta();
        return ApiResponse.success(Map.of("updated", updated));
    }

    // ==================== Videos ====================

    @PutMapping("/videos/{bvid}/meta")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> updateVideoMeta(@PathVariable String bvid,
                                              @RequestBody SongMetaRequest request) {
        videoService.setMetaOverride(bvid, request.getName(), request.getArtist());
        return ApiResponse.success();
    }

    // ==================== Helpers ====================

    private List<Long> getMusicIds() {
        String idsStr = siteSettingService.getAllSettings()
                .getOrDefault("music_ids", "").trim();
        if (idsStr.isEmpty()) return new ArrayList<>();
        return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void saveMusicIds(List<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        siteSettingService.setSetting("music_ids", joined);
    }

    @Data
    static class SongMetaRequest {
        private String name;
        private String artist;
    }
}
