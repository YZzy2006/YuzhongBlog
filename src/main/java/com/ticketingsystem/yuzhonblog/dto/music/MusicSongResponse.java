package com.ticketingsystem.yuzhonblog.dto.music;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MusicSongResponse {
    private Long id;
    private String name;
    private String artist;
    private String coverUrl;
    private String lyric;
    private String source;  // "netease" (default) or "bilibili"
    private String bvid;    // non-null only for Bilibili songs
    private Integer duration; // seconds, for bilibili songs
}
