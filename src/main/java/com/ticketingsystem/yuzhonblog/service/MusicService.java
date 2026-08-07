package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.dto.music.MusicSongResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MusicService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long CACHE_TTL_MS = 3600_000; // 1 hour

    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final HttpHeaders HEADERS = new HttpHeaders();
    static {
        HEADERS.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
        HEADERS.set("Referer", "https://music.163.com/");
    }

    private final RestTemplate rt = new RestTemplate() {{
        setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(5000);
            setReadTimeout(10000);
        }});
    }};

    public List<MusicSongResponse> getSongsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<MusicSongResponse> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                MusicSongResponse song = getSong(id);
                if (song != null) results.add(song);
            } catch (Exception e) {
                log.warn("获取歌曲 {} 失败: {}", id, e.getMessage());
            }
        }
        return results;
    }

    private MusicSongResponse getSong(Long id) throws Exception {
        CacheEntry cached = cache.get(id);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.song;
        }

        HttpEntity<Void> entity = new HttpEntity<>(HEADERS);

        // Fetch song detail
        String detailUrl = "https://music.163.com/api/song/detail/?id=" + id + "&ids=[" + id + "]";
        ResponseEntity<String> detailResp = rt.exchange(detailUrl, HttpMethod.GET, entity, String.class);
        JsonNode detailJson = MAPPER.readTree(detailResp.getBody());
        JsonNode songs = detailJson.path("songs");
        if (!songs.isArray() || songs.isEmpty()) {
            cache.put(id, new CacheEntry(null, System.currentTimeMillis()));
            return null;
        }

        JsonNode songNode = songs.get(0);
        String name = songNode.path("name").asText("未知歌曲");
        String artist = "未知歌手";
        JsonNode artists = songNode.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artist = artists.get(0).path("name").asText("未知歌手");
        }
        String coverUrl = songNode.path("album").path("picUrl").asText("");
        if (coverUrl.startsWith("http://")) coverUrl = coverUrl.replaceFirst("http://", "https://");

        // Fetch lyric (optional, failure doesn't affect main flow)
        String lrcText = "";
        try {
            String lrcUrl = "https://music.163.com/api/song/lyric?id=" + id + "&lv=-1&kv=-1&tv=-1";
            ResponseEntity<String> lrcResp = rt.exchange(lrcUrl, HttpMethod.GET, entity, String.class);
            JsonNode lrcJson = MAPPER.readTree(lrcResp.getBody());
            lrcText = lrcJson.path("lrc").path("lyric").asText("");
        } catch (Exception e) {
            log.debug("获取歌词失败 (id={}): {}", id, e.getMessage());
        }

        MusicSongResponse song = MusicSongResponse.builder()
                .id(id)
                .name(name)
                .artist(artist)
                .coverUrl(coverUrl)
                .lyric(lrcText)
                .build();

        cache.put(id, new CacheEntry(song, System.currentTimeMillis()));
        return song;
    }

    /**
     * 尝试将 ID 作为专辑查询，返回专辑信息（含歌曲 ID 列表）
     * 如果该 ID 不是有效的专辑，返回 null
     */
    public Map<String, Object> getAlbumInfo(Long albumId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(HEADERS);
            String url = "https://music.163.com/api/album/" + albumId;
            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode json = MAPPER.readTree(resp.getBody());
            JsonNode album = json.path("album");
            if (album.isMissingNode() || album.isNull()) return null;

            JsonNode songs = album.path("songs");
            if (!songs.isArray() || songs.isEmpty()) return null;

            String albumName = album.path("name").asText("");
            String artistName = "";
            JsonNode artist = album.path("artist");
            if (!artist.isMissingNode()) {
                artistName = artist.path("name").asText("");
            }
            String coverUrl = album.path("picUrl").asText("");
            if (coverUrl.startsWith("http://")) coverUrl = coverUrl.replaceFirst("http://", "https://");

            List<Long> ids = new ArrayList<>();
            for (JsonNode song : songs) {
                ids.add(song.path("id").asLong());
            }

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("albumName", albumName);
            result.put("artistName", artistName);
            result.put("coverUrl", coverUrl);
            result.put("songIds", ids);
            return result;
        } catch (Exception e) {
            log.debug("专辑查询失败 (albumId={}): {}", albumId, e.getMessage());
            return null;
        }
    }

    private static class CacheEntry {
        final MusicSongResponse song;
        final long timestamp;
        CacheEntry(MusicSongResponse song, long timestamp) {
            this.song = song;
            this.timestamp = timestamp;
        }
    }
}
