package com.ticketingsystem.yuzhonblog.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OssService {

    private final OssConfigService ossConfigService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv",
            "text/plain",
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_DOC_SIZE = 20 * 1024 * 1024; // 20MB

    public String uploadFile(MultipartFile file, String directory) {
        validateImageFile(file);

        OSS ossClient = ossConfigService.getOssClient();
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "OSS未配置，请先在后台配置阿里云OSS");
        }
        String bucketName = ossConfigService.getBucketName();
        String endpoint = ossConfigService.getEndpoint();
        String customDomain = ossConfigService.getCustomDomain();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = directory + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            // Prevent inline rendering of SVG files (can contain scripts)
            String lowerName = originalFilename != null ? originalFilename.toLowerCase() : "";
            if (lowerName.endsWith(".svg")) {
                metadata.setContentDisposition("attachment; filename=\"" + objectKey.substring(objectKey.lastIndexOf('/') + 1) + "\"");
            }
            ossClient.putObject(bucketName, objectKey, inputStream, metadata);
            ossClient.setObjectAcl(bucketName, objectKey, CannedAccessControlList.PublicRead);
            log.info("文件上传成功: {}", objectKey);

            if (customDomain != null && !customDomain.isBlank()) {
                String base = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
                return base + "/" + objectKey;
            }
            return "https://" + bucketName + "." + endpoint + "/" + objectKey;
        } catch (IOException | OSSException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public String uploadDocument(MultipartFile file, String directory) {
        validateDocumentFile(file);

        OSS ossClient = ossConfigService.getOssClient();
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "OSS未配置，请先在后台配置阿里云OSS");
        }
        String bucketName = ossConfigService.getBucketName();
        String endpoint = ossConfigService.getEndpoint();
        String customDomain = ossConfigService.getCustomDomain();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = directory + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            // Force download for documents
            metadata.setContentDisposition("attachment; filename=\"" + (originalFilename != null ? originalFilename : "file") + "\"");
            ossClient.putObject(bucketName, objectKey, inputStream, metadata);
            ossClient.setObjectAcl(bucketName, objectKey, CannedAccessControlList.PublicRead);
            log.info("文档上传成功: {}", objectKey);

            if (customDomain != null && !customDomain.isBlank()) {
                String base = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
                return base + "/" + objectKey;
            }
            return "https://" + bucketName + "." + endpoint + "/" + objectKey;
        } catch (IOException | OSSException e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public String uploadBytes(byte[] content, String contentType, String directory) {
        OSS ossClient = ossConfigService.getOssClient();
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "OSS未配置，请先在后台配置阿里云OSS");
        }
        String bucketName = ossConfigService.getBucketName();
        String endpoint = ossConfigService.getEndpoint();
        String customDomain = ossConfigService.getCustomDomain();

        String extension = contentType.contains("png") ? ".png" : ".jpg";
        String objectKey = directory + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = new java.io.ByteArrayInputStream(content)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(content.length);
            ossClient.putObject(bucketName, objectKey, inputStream, metadata);
            ossClient.setObjectAcl(bucketName, objectKey, CannedAccessControlList.PublicRead);
            log.info("文件上传成功: {}", objectKey);

            if (customDomain != null && !customDomain.isBlank()) {
                String base = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
                return base + "/" + objectKey;
            }
            return "https://" + bucketName + "." + endpoint + "/" + objectKey;
        } catch (IOException | OSSException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void deleteFile(String url) {
        if (url == null || url.isEmpty()) return;

        OSS ossClient = ossConfigService.getOssClient();
        if (ossClient == null) return;

        String bucketName = ossConfigService.getBucketName();
        String endpoint = ossConfigService.getEndpoint();
        String customDomain = ossConfigService.getCustomDomain();

        String prefix;
        if (customDomain != null && !customDomain.isBlank()) {
            prefix = customDomain.endsWith("/") ? customDomain : customDomain + "/";
        } else {
            prefix = "https://" + bucketName + "." + endpoint + "/";
        }

        if (!url.startsWith(prefix)) {
            log.warn("无效的OSS URL: {}", url);
            return;
        }

        String objectKey = url.substring(prefix.length());
        // Prevent path traversal attacks
        if (objectKey.contains("..")) {
            log.warn("检测到路径遍历尝试: {}", url);
            return;
        }
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        // Validate magic bytes to prevent Content-Type spoofing
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4 || !isImageMagicBytes(header, read)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "文件不能为空");
        }
        if (file.getSize() > MAX_DOC_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), "文档大小不能超过20MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOC_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), "不支持的文档类型，支持 PDF、Word、Excel、PPT、CSV、TXT、ZIP、RAR、7Z");
        }
        // Validate magic bytes for binary document types
        if (!"text/plain".equals(contentType) && !"text/csv".equals(contentType)) {
            try (InputStream is = file.getInputStream()) {
                byte[] header = new byte[8];
                int read = is.read(header);
                if (read < 4 || !isDocumentMagicBytes(header, read)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), "文件内容与声明的类型不匹配");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
    }

    private boolean isImageMagicBytes(byte[] header, int len) {
        if (len < 4) return false;
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47
        if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
        // GIF: 47 49 46 38
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return true;
        // WebP: RIFF....WEBP
        if (len >= 8 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[4] == 0x57 && header[5] == 0x45 && header[6] == 0x42 && header[7] == 0x50) return true;
        return false;
    }

    private boolean isDocumentMagicBytes(byte[] header, int len) {
        if (len < 4) return false;
        // PDF: 25 50 44 46 (%PDF)
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) return true;
        // ZIP / Office Open XML (docx/xlsx/pptx): 50 4B 03 04
        if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) return true;
        // OLE2 (doc/xls/ppt): D0 CF 11 E0
        if ((header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF && header[2] == 0x11 && (header[3] & 0xFF) == 0xE0) return true;
        // RAR: 52 61 72 21
        if (header[0] == 0x52 && header[1] == 0x61 && header[2] == 0x72 && header[3] == 0x21) return true;
        // 7Z: 37 7A BC AF
        if (header[0] == 0x37 && header[1] == 0x7A && (header[2] & 0xFF) == 0xBC && (header[3] & 0xFF) == 0xAF) return true;
        return false;
    }
}
