package com.aquarium.tank.dto;

/**
 * 资源冲突（如并发下唯一约束竞争失败），映射为 HTTP 409。
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
