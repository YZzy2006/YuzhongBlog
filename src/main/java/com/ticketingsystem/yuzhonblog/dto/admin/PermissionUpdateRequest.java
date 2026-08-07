package com.ticketingsystem.yuzhonblog.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PermissionUpdateRequest {

    @NotEmpty(message = "权限列表不能为空")
    @Valid
    private List<PermissionItem> permissions;

    @Data
    public static class PermissionItem {
        @NotNull(message = "权限代码不能为空")
        @Size(max = 100, message = "权限代码不能超过100字")
        private String permission;

        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
    }
}
