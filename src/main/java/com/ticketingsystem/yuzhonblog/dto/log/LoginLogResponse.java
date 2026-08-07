package com.ticketingsystem.yuzhonblog.dto.log;

import com.ticketingsystem.yuzhonblog.entity.LoginLog;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class LoginLogResponse {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String username;
    private String loginTime;
    private String deviceInfo;
    private String loginIp;
    private String location;
    private Integer status;
    private String failReason;

    public static LoginLogResponse from(LoginLog log) {
        LoginLogResponse r = new LoginLogResponse();
        r.id = log.getId();
        r.username = log.getUsername();
        r.loginTime = log.getCreatedAt() != null ? log.getCreatedAt().format(FMT) : null;
        r.deviceInfo = log.getDeviceInfo();
        r.loginIp = log.getLoginIp();
        r.location = log.getLocation();
        r.status = log.getStatus();
        r.failReason = log.getFailReason();
        return r;
    }
}
