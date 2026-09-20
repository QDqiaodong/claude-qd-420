package com.aquarium.tank.service;

import com.aquarium.tank.dto.BizException;
import com.aquarium.tank.dto.ConflictException;
import com.aquarium.tank.entity.Maintenance;
import com.aquarium.tank.entity.MaintenanceResult;
import com.aquarium.tank.entity.Tank;
import com.aquarium.tank.repository.MaintenanceRepository;
import com.aquarium.tank.repository.TankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    @Value("${app.business-zone:Asia/Shanghai}")
    private String businessZone;

    private final MaintenanceRepository maintenanceRepository;
    private final TankRepository tankRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, TankRepository tankRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.tankRepository = tankRepository;
    }

    public List<Maintenance> list() {
        return maintenanceRepository.findAll();
    }

    @Transactional
    public Maintenance create(Long tankId, LocalDate checkDate, String resultLabel, String note) {
        if (tankId == null) throw new BizException("展缸必填");
        Tank tank = tankRepository.findById(tankId).orElseThrow(() -> new BizException("展缸不存在"));
        if (checkDate == null) throw new BizException("巡检日期必填");
        if (resultLabel == null || resultLabel.isBlank()) throw new BizException("巡检结果必填");

        // 未来日期一律拒绝，日期口径以服务端业务时区为准，不相信客户端时钟
        LocalDate today = LocalDate.now(ZoneId.of(businessZone));
        if (checkDate.isAfter(today)) {
            throw new BizException("巡检日期不能晚于今天（" + today + "，服务端时区 " + businessZone + "）");
        }

        MaintenanceResult result = parseResult(resultLabel);
        String trimmedNote = note == null ? null : note.trim();
        if (result == MaintenanceResult.ABNORMAL && (trimmedNote == null || trimmedNote.isEmpty())) {
            throw new BizException("异常巡检必须填写备注");
        }

        // 友好预检：绝大多数重复登记在这里就被拦下
        if (maintenanceRepository.existsByTankIdAndCheckDate(tankId, checkDate)) {
            throw new ConflictException("展缸 " + tank.getCode() + " 在 " + checkDate
                    + " 已存在巡检记录，同一展缸同一天只能登记一次");
        }

        Maintenance m = new Maintenance();
        m.setTankId(tankId);
        m.setCheckDate(checkDate);
        m.setResult(result);
        m.setNote(trimmedNote);
        try {
            // 数据库唯一索引兜底：两名饲养员并发提交时，只有一笔 insert 能成功
            return maintenanceRepository.saveAndFlush(m);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueTankDateConflict(ex)) {
                log.info("并发巡检登记冲突：tankId={}, checkDate={}", tankId, checkDate);
                throw new ConflictException("展缸 " + tank.getCode() + " 在 " + checkDate
                        + " 的巡检刚刚已被其他饲养员登记，同一展缸同一天只能有一条记录，请刷新后查看");
            }
            throw ex;
        }
    }

    private boolean isUniqueTankDateConflict(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause == null ? "" : cause.getMessage();
        if (msg == null) msg = "";
        return msg.contains("uk_maintenance_tank_date")
                || (msg.contains("Duplicate entry") && msg.contains("maintenance"));
    }

    private MaintenanceResult parseResult(String label) {
        if ("正常".equals(label)) return MaintenanceResult.NORMAL;
        if ("异常".equals(label)) return MaintenanceResult.ABNORMAL;
        throw new BizException("巡检结果只能是「正常」或「异常」");
    }
}
