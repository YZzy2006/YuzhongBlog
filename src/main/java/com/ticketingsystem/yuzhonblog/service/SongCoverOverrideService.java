package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.entity.SongCoverOverride;
import com.ticketingsystem.yuzhonblog.repository.SongCoverOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongCoverOverrideService {

    private final SongCoverOverrideRepository repository;
    private final OssService ossService;

    @Transactional(readOnly = true)
    public Map<Long, String> getOverrides(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) return Map.of();
        return repository.findBySongIdIn(songIds).stream()
                .collect(Collectors.toMap(SongCoverOverride::getSongId, SongCoverOverride::getCustomCoverUrl));
    }

    @Transactional(readOnly = true)
    public Map<Long, SongCoverOverride> getFullOverrides(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) return Map.of();
        return repository.findBySongIdIn(songIds).stream()
                .collect(Collectors.toMap(SongCoverOverride::getSongId, o -> o));
    }

    @Transactional
    public void setOverride(Long songId, String coverUrl) {
        SongCoverOverride override = repository.findBySongId(songId)
                .orElseGet(() -> {
                    SongCoverOverride o = new SongCoverOverride();
                    o.setSongId(songId);
                    return o;
                });
        // Delete old custom cover from OSS if replacing
        if (override.getCustomCoverUrl() != null && !override.getCustomCoverUrl().isBlank()
                && !override.getCustomCoverUrl().equals(coverUrl)) {
            try {
                ossService.deleteFile(override.getCustomCoverUrl());
            } catch (Exception e) {
                log.warn("删除旧封面覆盖失败: {}", e.getMessage());
            }
        }
        override.setCustomCoverUrl(coverUrl);
        repository.save(override);
    }

    @Transactional
    public void setMetaOverride(Long songId, String customName, String customArtist) {
        SongCoverOverride override = repository.findBySongId(songId)
                .orElseGet(() -> {
                    SongCoverOverride o = new SongCoverOverride();
                    o.setSongId(songId);
                    o.setCustomCoverUrl("");
                    return o;
                });
        override.setCustomName(customName);
        override.setCustomArtist(customArtist);
        repository.save(override);
    }

    @Transactional
    public void removeOverride(Long songId) {
        repository.findBySongId(songId).ifPresent(override -> {
            if (override.getCustomCoverUrl() != null && !override.getCustomCoverUrl().isBlank()) {
                try {
                    ossService.deleteFile(override.getCustomCoverUrl());
                } catch (Exception e) {
                    log.warn("删除封面覆盖OSS文件失败: {}", e.getMessage());
                }
            }
            repository.delete(override);
        });
    }
}
