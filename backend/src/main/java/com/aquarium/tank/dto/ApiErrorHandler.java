package com.aquarium.tank.dto;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handle(BizException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handle(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handle(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handle(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "message", "请求数据格式有误，请检查提交内容"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handle(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (detail != null && (detail.contains("Duplicate entry") || detail.contains("UNIQUE"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("ok", false, "message", "记录已存在或与已有数据冲突，请刷新后重试"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "message", "数据不满足完整性约束，请检查提交内容"));
    }
}
