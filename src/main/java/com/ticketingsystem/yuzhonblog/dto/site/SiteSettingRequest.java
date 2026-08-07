package com.ticketingsystem.yuzhonblog.dto.site;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class SiteSettingRequest {
    @Size(max = 100, message = "设置项数量不能超过100")
    private Map<String, @Size(max = 10000, message = "单个设置值不能超过10000字") String> settings;
}
