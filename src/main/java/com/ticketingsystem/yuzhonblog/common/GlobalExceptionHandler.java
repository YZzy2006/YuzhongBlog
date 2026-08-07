package com.ticketingsystem.yuzhonblog.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        int httpStatus = mapToHttpStatus(e.getCode());
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    private int mapToHttpStatus(int code) {
        if (code == 401 || code == 403 || code == 404) return code;
        // Auth: token/credential errors → 401 (frontend interceptor relies on this)
        if (code == 1001 || code == 1002 || code == 1003) return 401;
        // Session conflict → 409 (frontend has special handling)
        if (code == 1006) return 409;
        // Validation / input errors
        if (code == 1007 || code == 1008 || code == 1101 || code == 1102 || code == 1107) return 400;
        // Account status / permission denied
        if (code == 1004 || code == 1009 || code == 1104 || code == 1105 || code == 1106) return 403;
        // Not found
        if (code == 1103 || code == 4001) return 404;
        return 400;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超过大小限制: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), "文件大小不能超过5MB");
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        log.debug("客户端断开连接: {}", e.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("SSE连接中断: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        log.error("未知异常", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }
}
