package com.aquarium.tank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "feeding")
@Getter
@Setter
public class Feeding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tank_id", nullable = false)
    private Long tankId;

    @Column(name = "feed_date", nullable = false)
    private LocalDate feedDate;

    @Column(nullable = false)
    private String food;

    @Column(nullable = false)
    private Integer qty;
}
