package com.aquarium.tank.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
