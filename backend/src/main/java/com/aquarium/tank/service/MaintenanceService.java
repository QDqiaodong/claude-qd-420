package com.aquarium.tank.service;

import com.aquarium.tank.dto.BizException;
import com.aquarium.tank.entity.Maintenance;
import com.aquarium.tank.entity.MaintenanceResult;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.repository.MaintenanceRepository;
import com.aquarium.tank.repository.TankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceService {

    @Autowired
    private MaintenanceRepository maintenanceRepository;
    @Autowired
    private TankRepository tankRepository;

    public List<Maintenance> list() {
        return maintenanceRepository.findAll();
    }

    @Transactional
    public Maintenance create(Long tankId, LocalDate checkDate, String resultLabel, String note) {
        if (tankId == null) throw new BizException("展缸必填");
        Tank tank = tankRepository.findById(tankId).orElseThrow(() -> new BizException("展缸不存在"));
        if (checkDate == null) throw new BizException("巡检日期必填");
        if (resultLabel == null || resultLabel.isBlank()) throw new BizException("巡检结果必填");
        MaintenanceResult result = parseResult(resultLabel);
        if (result == MaintenanceResult.ABNORMAL && (note == null || note.isBlank()))
            throw new BizException("异常巡检必须填写备注");

        Maintenance m = new Maintenance();
        m.setTankId(tankId);
        m.setCheckDate(checkDate);
        m.setResult(result);
        m.setNote(note);
        return maintenanceRepository.save(m);
    }

    private MaintenanceResult parseResult(String label) {
        if ("异常".equals(label)) return MaintenanceResult.ABNORMAL;
        return MaintenanceResult.NORMAL;
    }
}
