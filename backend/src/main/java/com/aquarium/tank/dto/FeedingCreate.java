package com.aquarium.tank.dto;

import java.time.LocalDate;

public record FeedingCreate(Long tankId, LocalDate feedDate, String food, Integer qty) {
}
