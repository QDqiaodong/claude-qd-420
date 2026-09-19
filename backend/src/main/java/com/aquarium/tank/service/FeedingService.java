package com.aquarium.tank.service;

import com.aquarium.tank.dto.BizException;
import com.aquarium.tank.entity.Feeding;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.entity.TankState;
import com.aquarium.tank.repository.FeedingRepository;
import com.aquarium.tank.repository.TankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FeedingService {

    @Autowired
    private FeedingRepository feedingRepository;
    @Autowired
    private TankRepository tankRepository;

    public List<Feeding> list() {
        return feedingRepository.findAll();
    }

    @Transactional
    public Feeding create(Long tankId, LocalDate feedDate, String food, Integer qty) {
        if (tankId == null) throw new BizException("展缸必填");
        Tank tank = tankRepository.findById(tankId).orElseThrow(() -> new BizException("展缸不存在"));
        if (tank.getState() != TankState.NORMAL) throw new BizException("维护/停用展缸禁止投喂");
        if (feedDate == null) throw new BizException("投喂日期必填");
        if (food == null || food.isBlank()) throw new BizException("饲料必填");
        if (qty == null || qty <= 0) throw new BizException("数量必须大于 0");

        Feeding f = new Feeding();
        f.setTankId(tankId);
        f.setFeedDate(feedDate);
        f.setFood(food);
        f.setQty(qty);
        return feedingRepository.save(f);
    }
}
