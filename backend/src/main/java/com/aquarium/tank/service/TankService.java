package com.aquarium.tank.service;

import com.aquarium.tank.dto.BizException;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.entity.TankState;
import com.aquarium.tank.repository.TankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TankService {

    @Autowired
    private TankRepository tankRepository;

    public List<Tank> list() {
        return tankRepository.findAll();
    }

    @Transactional
    public Tank create(String code, Integer capacity) {
        if (code == null || code.isBlank()) throw new BizException("展缸编号必填");
        if (tankRepository.existsByCode(code)) throw new BizException("展缸编号 " + code + " 已存在");
        if (capacity == null || capacity <= 0) throw new BizException("容量必须大于 0");
        Tank tank = new Tank();
        tank.setCode(code);
        tank.setCapacity(capacity);
        tank.setState(TankState.NORMAL); // 默认值放在 create 里设
        return tankRepository.save(tank);
    }

    @Transactional
    public Tank updateCapacity(Long id, Integer capacity) {
        Tank tank = tankRepository.findById(id).orElseThrow(() -> new BizException("展缸不存在"));
        if (capacity == null || capacity <= 0) throw new BizException("容量必须大于 0");
        if (capacity < tank.getCreatureCount()) throw new BizException("容量不能小于当前生物数");
        tank.setCapacity(capacity);
        return tankRepository.save(tank);
    }

    @Transactional
    public Tank startMaintenance(Long id) {
        Tank tank = tankRepository.findById(id).orElseThrow(() -> new BizException("展缸不存在"));
        tank.startMaintenance(); // 状态机逻辑在实体方法里
        return tankRepository.save(tank);
    }

    @Transactional
    public Tank finishMaintenance(Long id) {
        Tank tank = tankRepository.findById(id).orElseThrow(() -> new BizException("展缸不存在"));
        tank.finishMaintenance();
        return tankRepository.save(tank);
    }

    @Transactional
    public Tank disable(Long id) {
        Tank tank = tankRepository.findById(id).orElseThrow(() -> new BizException("展缸不存在"));
        tank.disable(); // 含 assertCanDisable 校验
        return tankRepository.save(tank);
    }
}
