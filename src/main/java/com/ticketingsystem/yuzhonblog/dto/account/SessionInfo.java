package com.ticketingsystem.yuzhonblog.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionInfo {
    private String deviceInfo;
    private String loginTime;
    private String loginIp;
}
