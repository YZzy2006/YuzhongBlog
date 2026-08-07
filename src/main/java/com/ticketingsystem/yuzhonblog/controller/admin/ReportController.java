package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.entity.Report;
import com.ticketingsystem.yuzhonblog.repository.ReportRepository;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;

    @GetMapping
    @RequirePermission("setting:view")
    public ApiResponse<Page<Report>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 100);
        return ApiResponse.success(reportRepository.search(type, keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission("setting:view")
    public ApiResponse<Report> get(@PathVariable Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new com.ticketingsystem.yuzhonblog.common.BusinessException(
                        com.ticketingsystem.yuzhonblog.common.ErrorCode.NOT_FOUND));
        return ApiResponse.success(report);
    }

    @PostMapping
    @RequirePermission("setting:edit")
    public ApiResponse<Report> save(@Valid @RequestBody SaveReportRequest request, Authentication auth) {
        Report report = new Report();
        report.setReportType(request.getReportType());
        report.setTitle(request.getTitle());
        report.setContent(request.getContent());
        report.setDataSnapshot(request.getDataSnapshot());
        report.setReportDate(request.getReportDate());
        report.setCreatedBy(auth != null ? auth.getName() : "unknown");
        return ApiResponse.success(reportRepository.save(report));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reportRepository.deleteById(id);
        return ApiResponse.success(null);
    }

    @Data
    public static class SaveReportRequest {
        @jakarta.validation.constraints.NotBlank
        private String reportType;
        @jakarta.validation.constraints.NotBlank
        private String title;
        @jakarta.validation.constraints.Size(max = 50000, message = "内容不能超过50000字")
        private String content;
        @jakarta.validation.constraints.Size(max = 50000, message = "数据快照不能超过50000字")
        private String dataSnapshot;
        private LocalDate reportDate;
    }
}
