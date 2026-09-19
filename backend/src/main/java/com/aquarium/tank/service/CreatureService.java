package com.aquarium.tank.service;

import com.aquarium.tank.dto.BizException;
import com.aquarium.tank.entity.Creature;
import com.aquarium.tank.entity.CreatureStatus;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.entity.TankState;
import com.aquarium.tank.repository.CreatureRepository;
import com.aquarium.tank.repository.TankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreatureService {

    @Autowired
    private CreatureRepository creatureRepository;
    @Autowired
    private TankRepository tankRepository;

    public List<Creature> list() {
        return creatureRepository.findAll();
    }

    @Transactional
    public Creature create(Long tankId, String species, String name, String statusLabel) {
        if (tankId == null) throw new BizException("归属展缸必填");
        Tank tank = tankRepository.findById(tankId).orElseThrow(() -> new BizException("展缸不存在"));
        if (tank.getState() != TankState.NORMAL) throw new BizException("仅「正常」展缸可归属生物");
        if (tank.getCreatureCount() >= tank.getCapacity()) throw new BizException("展缸生物数已达容量上限");
        if (species == null || species.isBlank()) throw new BizException("品种必填");
        if (name == null || name.isBlank()) throw new BizException("名称必填");

        Creature c = new Creature();
        c.setTank(tank);
        c.setSpecies(species);
        c.setName(name);
        c.setStatus(parseStatus(statusLabel));

        // 通过 @OneToMany(cascade = CascadeType.ALL) 由展缸级联保存生物
        tank.getCreatures().add(c);
        tankRepository.save(tank);
        return c;
    }

    @Transactional
    public Creature setStatus(Long id, String statusLabel) {
        Creature c = creatureRepository.findById(id).orElseThrow(() -> new BizException("生物不存在"));
        c.setStatus(parseStatus(statusLabel));
        return creatureRepository.save(c);
    }

    @Transactional
    public void remove(Long id) {
        Creature c = creatureRepository.findById(id).orElseThrow(() -> new BizException("生物不存在"));
        Tank tank = c.getTank();
        tank.getCreatures().remove(c);
        // orphanRemoval = true：从集合中移除即级联删除
        tankRepository.save(tank);
    }

    private CreatureStatus parseStatus(String label) {
        if ("下架".equals(label)) return CreatureStatus.OFF;
        return CreatureStatus.ON_DISPLAY;
    }
}
