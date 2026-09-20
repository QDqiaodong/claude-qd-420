package com.aquarium.tank.controller;

import com.aquarium.tank.dto.MaintenanceCreate;
import com.aquarium.tank.entity.Maintenance;
import com.aquarium.tank.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceController {

    @Autowired
    private MaintenanceService service;

    @GetMapping
    public List<Maintenance> list() {
        return service.list();
    }

    @PostMapping
    public Maintenance create(@Valid @RequestBody MaintenanceCreate f) {
        return service.create(f.tankId(), f.checkDate(), f.result(), f.note());
    }
}
