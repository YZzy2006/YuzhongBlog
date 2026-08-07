package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.entity.MusicCustomSong;
import com.ticketingsystem.yuzhonblog.repository.MusicCustomSongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicCustomSongService {

    private final MusicCustomSongRepository repository;
    private final OssService ossService;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpHeaders BILI_HEADERS = new HttpHeaders();
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    static {
        BILI_HEADERS.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        BILI_HEADERS.set("Referer", "https://www.bilibili.com/");
        REST_TEMPLATE.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(5000);
            setReadTimeout(10000);
        }});
    }

    @Transactional(readOnly = true)
    public List<MusicCustomSong> listAll() {
        return repository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public MusicCustomSong add(String bvid) {
        if (bvid == null || !bvid.matches("^BV[a-zA-Z0-9]{10}$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "无效的BV号格式");
        }
        if (repository.existsBySourceId(bvid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该B站视频已在歌曲列表中");
        }

        Map<String, String> meta = fetchBilibiliMeta(bvid);
        MusicCustomSong song = new MusicCustomSong();
        song.setSourceType("bilibili");
        song.setSourceId(bvid);
        song.setTitle(meta.getOrDefault("title", "未知歌曲"));
        song.setArtist(meta.getOrDefault("author", "未知UP主"));
        song.setCoverUrl(meta.getOrDefault("cover", ""));
        song.setSortOrder((int) repository.count());
        try {
            song.setDuration(Integer.parseInt(meta.getOrDefault("duration", "0")));
        } catch (NumberFormatException e) {
            song.setDuration(0);
        }
        MusicCustomSong saved = repository.save(song);
        // Auto-crop cover to center square
        autoCropCover(saved);
        return saved;
    }

    @Transactional
    public void remove(Long id) {
        MusicCustomSong song = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        // Delete custom cover from OSS if present
        if (song.getCustomCoverUrl() != null && !song.getCustomCoverUrl().isBlank()) {
            try {
                ossService.deleteFile(song.getCustomCoverUrl());
            } catch (Exception e) {
                log.warn("删除自定义封面失败: {}", e.getMessage());
            }
        }
        repository.delete(song);
    }

    @Transactional
    public int refreshAllMeta() {
        List<MusicCustomSong> songs = repository.findAll();
        int updated = 0;
        for (MusicCustomSong song : songs) {
            try {
                Map<String, String> meta = fetchBilibiliMeta(song.getSourceId());
                song.setTitle(meta.getOrDefault("title", song.getTitle()));
                song.setArtist(meta.getOrDefault("author", song.getArtist()));
                if (song.getCoverUrl() == null || song.getCoverUrl().isBlank()) {
                    song.setCoverUrl(meta.getOrDefault("cover", ""));
                }
                // Auto-crop if no custom cover yet
                if (song.getCustomCoverUrl() == null || song.getCustomCoverUrl().isBlank()) {
                    autoCropCover(song);
                }
                try {
                    song.setDuration(Integer.parseInt(meta.getOrDefault("duration", "0")));
                } catch (NumberFormatException e) {
                    // keep existing duration
                }
                repository.save(song);
                updated++;
            } catch (Exception e) {
                log.warn("刷新歌曲元数据失败: bvid={}, error={}", song.getSourceId(), e.getMessage());
            }
        }
        return updated;
    }

    public int fetchDuration(String bvid) {
        try {
            String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
            ResponseEntity<String> resp = REST_TEMPLATE.exchange(url, HttpMethod.GET, new HttpEntity<>(BILI_HEADERS), String.class);
            JsonNode json = MAPPER.readTree(resp.getBody());
            if (json.path("code").asInt(-1) == 0) {
                return json.path("data").path("duration").asInt(0);
            }
        } catch (Exception e) {
            log.warn("获取B站视频时长失败: bvid={}", bvid);
        }
        return 0;
    }

    @Transactional
    public void save(MusicCustomSong song) {
        repository.save(song);
    }

    @Transactional
    public void reorder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            int sortOrder = i;
            repository.findById(orderedIds.get(i)).ifPresent(song -> {
                song.setSortOrder(sortOrder);
                repository.save(song);
            });
        }
    }

    @Transactional
    public void updateMeta(Long id, String title, String artist) {
        MusicCustomSong song = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (title != null && !title.isBlank()) {
            song.setTitle(title.trim());
        }
        if (artist != null && !artist.isBlank()) {
            song.setArtist(artist.trim());
        }
        repository.save(song);
    }

    @Transactional
    public void updateCover(Long id, String coverUrl) {
        MusicCustomSong song = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        // Delete old custom cover from OSS if replacing
        if (song.getCustomCoverUrl() != null && !song.getCustomCoverUrl().isBlank()) {
            try {
                ossService.deleteFile(song.getCustomCoverUrl());
            } catch (Exception e) {
                log.warn("删除旧自定义封面失败: {}", e.getMessage());
            }
        }
        song.setCustomCoverUrl(coverUrl);
        repository.save(song);
    }

    public Map<String, String> queryBilibili(String bvid) {
        if (bvid == null || !bvid.matches("^BV[a-zA-Z0-9]{10}$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "无效的BV号格式");
        }
        return fetchBilibiliMeta(bvid);
    }

    private void autoCropCover(MusicCustomSong song) {
        String coverUrl = song.getCoverUrl();
        if (coverUrl == null || coverUrl.isBlank()) return;
        try {
            ResponseEntity<byte[]> resp = REST_TEMPLATE.exchange(
                    coverUrl, HttpMethod.GET, new HttpEntity<>(BILI_HEADERS), byte[].class);
            byte[] imageBytes = resp.getBody();
            if (imageBytes == null || imageBytes.length == 0) return;

            BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (original == null) return;

            int w = original.getWidth();
            int h = original.getHeight();
            if (w == h) return; // already square

            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            BufferedImage cropped = original.getSubimage(x, y, size, size);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "png", baos);
            String url = ossService.uploadBytes(baos.toByteArray(), "image/png", "music-cover");
            song.setCustomCoverUrl(url);
            repository.save(song);
            log.info("B站封面自动裁剪成功: bvid={}", song.getSourceId());
        } catch (Exception e) {
            log.warn("B站封面自动裁剪失败，使用原始封面: bvid={}, error={}", song.getSourceId(), e.getMessage());
        }
    }

    private Map<String, String> fetchBilibiliMeta(String bvid) {
        try {
            String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
            ResponseEntity<String> resp = REST_TEMPLATE.exchange(url, HttpMethod.GET, new HttpEntity<>(BILI_HEADERS), String.class);
            JsonNode json = MAPPER.readTree(resp.getBody());

            int code = json.path("code").asInt(-1);
            if (code != 0) {
                String msg = json.path("message").asText("unknown error");
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "B站API错误: " + msg);
            }

            JsonNode data = json.path("data");
            String title = data.path("title").asText("未知视频");
            String cover = data.path("pic").asText("");
            if (cover.startsWith("http://")) cover = cover.replaceFirst("http://", "https://");
            String author = data.path("owner").path("name").asText("未知UP主");
            String duration = String.valueOf(data.path("duration").asInt(0));

            return Map.of("title", title, "cover", cover, "author", author, "duration", duration);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("获取B站视频元数据失败: bvid={}, error={}", bvid, e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "获取B站视频信息失败");
        }
    }
}
