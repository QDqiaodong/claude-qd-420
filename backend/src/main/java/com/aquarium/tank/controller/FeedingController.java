package com.aquarium.tank.controller;

import com.aquarium.tank.dto.FeedingCreate;
import com.aquarium.tank.entity.Feeding;
import com.aquarium.tank.service.FeedingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedings")
public class FeedingController {

    @Autowired
    private FeedingService service;

    @GetMapping
    public List<Feeding> list() {
        return service.list();
    }

    @PostMapping
    public Feeding create(@RequestBody FeedingCreate f) {
        return service.create(f.tankId(), f.feedDate(), f.food(), f.qty());
    }
}
