package com.aquarium.tank.controller;

import com.aquarium.tank.dto.TankCreate;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.service.TankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tanks")
public class TankController {

    @Autowired
    private TankService service;

    @GetMapping
    public List<Tank> list() {
        return service.list();
    }

    @PostMapping
    public Tank create(@RequestBody TankCreate f) {
        return service.create(f.code(), f.capacity());
    }

    @PutMapping("/{id}/maintenance")
    public Tank startMaintenance(@PathVariable Long id) {
        return service.startMaintenance(id);
    }

    @PutMapping("/{id}/finish")
    public Tank finishMaintenance(@PathVariable Long id) {
        return service.finishMaintenance(id);
    }

    @PutMapping("/{id}/disable")
    public Tank disable(@PathVariable Long id) {
        return service.disable(id);
    }

    @PutMapping("/{id}/capacity")
    public Tank updateCapacity(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        return service.updateCapacity(id, body.get("capacity"));
    }
}
