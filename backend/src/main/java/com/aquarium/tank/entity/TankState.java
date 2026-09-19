package com.aquarium.tank.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TankState {
    NORMAL("正常"),
    MAINTENANCE("维护"),
    DISABLED("停用");

    private final String label;

    TankState(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
