package com.ticketingsystem.yuzhonblog.dto.log;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeatmapEntry {
    private String date;
    private long count;
}
