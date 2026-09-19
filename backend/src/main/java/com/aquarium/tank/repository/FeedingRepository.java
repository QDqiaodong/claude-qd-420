package com.aquarium.tank.repository;

import com.aquarium.tank.entity.Feeding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedingRepository extends JpaRepository<Feeding, Long> {
}
