package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.music.MusicSongResponse;
import com.ticketingsystem.yuzhonblog.entity.MusicCustomSong;
import com.ticketingsystem.yuzhonblog.entity.SongCoverOverride;
import com.ticketingsystem.yuzhonblog.service.MusicCustomSongService;
import com.ticketingsystem.yuzhonblog.service.MusicService;
import com.ticketingsystem.yuzhonblog.service.SiteSettingService;
import com.ticketingsystem.yuzhonblog.service.SongCoverOverrideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;
    private final SiteSettingService siteSettingService;
    private final MusicCustomSongService customSongService;
    private final SongCoverOverrideService coverOverrideService;

    @GetMapping("/songs")
    public ApiResponse<List<MusicSongResponse>> songs() {
        Map<String, String> allSettings = siteSettingService.getAllSettings();
        List<MusicSongResponse> result = new ArrayList<>();

        // 1. NetEase songs
        String idsStr = allSettings.getOrDefault("music_ids", "").trim();
        if (!idsStr.isEmpty()) {
            List<Long> ids = Arrays.stream(idsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<MusicSongResponse> neteaseSongs = musicService.getSongsByIds(ids);

            // Apply overrides
            Map<Long, SongCoverOverride> overrides = coverOverrideService.getFullOverrides(ids);
            for (MusicSongResponse song : neteaseSongs) {
                SongCoverOverride override = overrides.get(song.getId());
                if (override != null) {
                    if (override.getCustomCoverUrl() != null && !override.getCustomCoverUrl().isBlank()) {
                        song.setCoverUrl(override.getCustomCoverUrl());
                    }
                    if (override.getCustomName() != null && !override.getCustomName().isBlank()) {
                        song.setName(override.getCustomName());
                    }
                    if (override.getCustomArtist() != null && !override.getCustomArtist().isBlank()) {
                        song.setArtist(override.getCustomArtist());
                    }
                }
                song.setSource("netease");
            }
            result.addAll(neteaseSongs);
        }

        // 2. Bilibili songs
        List<MusicCustomSong> customSongs = customSongService.listAll();
        for (MusicCustomSong cs : customSongs) {
            String coverUrl = (cs.getCustomCoverUrl() != null && !cs.getCustomCoverUrl().isBlank())
                    ? cs.getCustomCoverUrl() : cs.getCoverUrl();
            int dur = cs.getDuration() != null ? cs.getDuration() : 0;
            result.add(MusicSongResponse.builder()
                    .id(cs.getId())
                    .name(cs.getTitle())
                    .artist(cs.getArtist())
                    .coverUrl(coverUrl)
                    .lyric("")
                    .source("bilibili")
                    .bvid(cs.getSourceId())
                    .duration(dur)
                    .build());
        }

        return ApiResponse.success(result);
    }
}
