package com.aquarium.tank.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CreatureStatus {
    ON_DISPLAY("在展"),
    OFF("下架");

    private final String label;

    CreatureStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
