package com.aquarium.tank.controller;

import com.aquarium.tank.dto.CreatureCreate;
import com.aquarium.tank.entity.Creature;
import com.aquarium.tank.service.CreatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creatures")
public class CreatureController {

    @Autowired
    private CreatureService service;

    @GetMapping
    public List<Creature> list() {
        return service.list();
    }

    @PostMapping
    public Creature create(@RequestBody CreatureCreate f) {
        return service.create(f.tankId(), f.species(), f.name(), f.status());
    }

    @PutMapping("/{id}/status")
    public Creature setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.setStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable Long id) {
        service.remove(id);
        return Map.of("ok", true);
    }
}
