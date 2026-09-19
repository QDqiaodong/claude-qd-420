package com.aquarium.tank.dto;

import java.time.LocalDate;

public record MaintenanceCreate(Long tankId, LocalDate checkDate, String result, String note) {
}
