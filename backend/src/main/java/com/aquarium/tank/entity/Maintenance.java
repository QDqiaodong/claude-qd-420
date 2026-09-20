package com.aquarium.tank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_maintenance_tank_date", columnNames = {"tank_id", "check_date"})
})
@Getter
@Setter
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tank_id", nullable = false)
    private Long tankId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceResult result;

    @Column
    private String note;
}
