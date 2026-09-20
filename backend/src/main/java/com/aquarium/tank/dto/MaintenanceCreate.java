package com.aquarium.tank.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MaintenanceCreate(
        @NotNull(message = "展缸必填") Long tankId,
        @NotNull(message = "巡检日期必填") LocalDate checkDate,
        String result,
        String note) {
}
