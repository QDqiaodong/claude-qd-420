package com.aquarium.tank.repository;

import com.aquarium.tank.entity.Tank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TankRepository extends JpaRepository<Tank, Long> {
    boolean existsByCode(String code);

    Optional<Tank> findByCode(String code);
}
