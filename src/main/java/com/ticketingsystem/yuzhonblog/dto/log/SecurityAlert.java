package com.ticketingsystem.yuzhonblog.dto.log;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecurityAlert {
    private String username;
    private long failCount;
    private String lastFailTime;
}
