package com.ticketingsystem.yuzhonblog.dto.account;

import com.ticketingsystem.yuzhonblog.dto.log.HeatmapEntry;
import com.ticketingsystem.yuzhonblog.dto.log.SecurityAlert;
import lombok.Data;

import java.util.List;

@Data
public class AccountSecurityResponse {
    private SessionInfo currentSession;
    private List<HeatmapEntry> heatmap;
    private boolean phoneBound;
    private String maskedPhone;
    private String username;
    private String realName;
    private String email;
    private String role;
    private List<SecurityAlert> securityAlerts;
}
