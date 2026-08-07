package com.ticketingsystem.yuzhonblog.dto.site;

import lombok.Data;
import java.util.Map;

@Data
public class SiteInfoResponse {
    private String siteName;
    private String siteDescription;
    private Map<String, String> extraSettings;
}
