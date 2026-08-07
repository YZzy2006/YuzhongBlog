package com.ticketingsystem.yuzhonblog.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OssConfigRequest {
    @Size(max = 200, message = "Endpoint不能超过200字")
    private String endpoint;
    @Size(max = 100, message = "Bucket名称不能超过100字")
    private String bucketName;
    @Size(max = 200, message = "AccessKey ID不能超过200字")
    private String accessKeyId;
    @Size(max = 200, message = "AccessKey Secret不能超过200字")
    private String accessKeySecret;
    @Size(max = 200, message = "自定义域名不能超过200字")
    private String customDomain;
}
