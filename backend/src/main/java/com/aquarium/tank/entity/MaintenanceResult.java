package com.aquarium.tank.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MaintenanceResult {
    NORMAL("正常"),
    ABNORMAL("异常");

    private final String label;

    MaintenanceResult(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
