package com.ticketingsystem.yuzhonblog.dto.backup;

import lombok.Data;

import java.util.Map;

@Data
public class ImportSummaryResponse {
    private Map<String, Integer> counts;
    private int totalRecords;
}
