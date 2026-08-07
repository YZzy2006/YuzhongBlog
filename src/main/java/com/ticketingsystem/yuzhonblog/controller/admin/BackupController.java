package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.backup.BackupResponse;
import com.ticketingsystem.yuzhonblog.dto.backup.ImportSummaryResponse;
import com.ticketingsystem.yuzhonblog.entity.BackupRecord;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    @RequirePermission("backup:manage")
    public ApiResponse<BackupResponse> create(
            @RequestParam(required = false) String description) {
        return ApiResponse.success(backupService.createBackup(description));
    }

    @GetMapping
    @RequirePermission("backup:manage")
    public ApiResponse<PageResult<BackupResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 100);
        return ApiResponse.success(backupService.listBackups(page, size));
    }

    @GetMapping("/{id}/download")
    @RequirePermission("backup:manage")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        BackupRecord record = backupService.getBackup(id);
        byte[] data = backupService.downloadBackup(id);
        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(record.getFilename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @DeleteMapping
    @RequirePermission("backup:manage")
    public ApiResponse<Void> bulkDelete(@RequestParam List<Long> ids) {
        backupService.deleteBackups(ids);
        return ApiResponse.success();
    }

    @PostMapping("/import")
    @RequirePermission("backup:manage")
    public ApiResponse<ImportSummaryResponse> importBackup(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                    com.ticketingsystem.yuzhonblog.common.ErrorCode.BACKUP_IMPORT_INVALID);
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                    com.ticketingsystem.yuzhonblog.common.ErrorCode.BACKUP_IMPORT_INVALID.getCode(),
                    "备份文件不能超过50MB");
        }
        return ApiResponse.success(backupService.importBackup(file));
    }
}
