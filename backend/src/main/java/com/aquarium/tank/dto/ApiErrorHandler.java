package com.aquarium.tank.dto;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handle(BizException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handle(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handle(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handle(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return badRequest(detail.isEmpty() ? "请求参数校验失败" : detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handle(HttpMessageNotReadableException ex) {
        return badRequest("请求内容无法解析，请检查日期等字段格式（日期格式需为 YYYY-MM-DD）");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handle(DataIntegrityViolationException ex) {
        String reason = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (reason.contains("Duplicate entry") || reason.contains("uk_maintenance_tank_date")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(body("该展缸当天的巡检记录已存在，请勿重复登记"));
        }
        return badRequest("数据违反唯一或约束规则：" + reason);
    }

    private String formatFieldError(FieldError fe) {
        String message = fe.getDefaultMessage();
        return fe.getField() + ": " + (message == null ? "不合法" : message);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(message));
    }

    private Map<String, Object> body(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("message", message);
        return m;
    }
}
