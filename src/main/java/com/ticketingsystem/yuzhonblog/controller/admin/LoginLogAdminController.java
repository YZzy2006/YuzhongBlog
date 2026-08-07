package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.log.HeatmapEntry;
import com.ticketingsystem.yuzhonblog.dto.log.LoginLogResponse;
import com.ticketingsystem.yuzhonblog.dto.log.SecurityAlert;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.LoginLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/login-logs")
@RequiredArgsConstructor
public class LoginLogAdminController {

    private final LoginLogService loginLogService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @RequirePermission("log:view")
    public ApiResponse<PageResult<LoginLogResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ApiResponse.success(loginLogService.search(keyword, status, startDate, endDate, page, size));
    }

    @GetMapping("/export")
    @RequirePermission("log:view")
    public void exportCsv(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate,
                          @RequestParam(required = false) Integer page,
                          @RequestParam(required = false) Integer size,
                          HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=login-logs.csv");
        response.setCharacterEncoding("UTF-8");
        // BOM for Excel
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        List<LoginLogResponse> logs = loginLogService.exportCsv(keyword, status, startDate, endDate, page, size);
        PrintWriter writer = response.getWriter();
        writer.println("ID,用户名,登录时间,设备信息,登录IP,地区,状态,失败原因");
        for (LoginLogResponse log : logs) {
            writer.println(String.join(",",
                    safeStr(String.valueOf(log.getId())),
                    safeStr(log.getUsername()),
                    safeStr(log.getLoginTime()),
                    safeStr(log.getDeviceInfo()),
                    safeStr(log.getLoginIp()),
                    safeStr(log.getLocation()),
                    log.getStatus() == 1 ? "成功" : "失败",
                    safeStr(log.getFailReason())
            ));
        }
        writer.flush();
    }

    @GetMapping("/heatmap")
    @RequirePermission("log:view")
    public ApiResponse<List<HeatmapEntry>> heatmap() {
        Long userId = currentUserProvider.getCurrentUser().getId();
        return ApiResponse.success(loginLogService.getLoginHeatmap(userId));
    }

    @GetMapping("/security-alerts")
    @RequirePermission("log:view")
    public ApiResponse<List<SecurityAlert>> securityAlerts() {
        return ApiResponse.success(loginLogService.getSecurityAlerts());
    }

    private String safeStr(String s) {
        if (s == null) return "";
        // Prevent CSV formula injection
        if (!s.isEmpty() && (s.charAt(0) == '=' || s.charAt(0) == '+' || s.charAt(0) == '-' || s.charAt(0) == '@')) {
            s = "'" + s;
        }
        // Escape CSV special characters
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
