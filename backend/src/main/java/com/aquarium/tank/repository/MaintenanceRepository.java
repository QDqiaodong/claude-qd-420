package com.aquarium.tank.repository;

import com.aquarium.tank.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    boolean existsByTankIdAndCheckDate(Long tankId, LocalDate checkDate);
}
