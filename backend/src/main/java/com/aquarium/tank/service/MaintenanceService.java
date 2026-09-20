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

import java.time.Clock;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final MaintenanceRepository maintenanceRepository;
    private final TankRepository tankRepository;
    private final ZoneId businessZone;

    public MaintenanceService(MaintenanceRepository maintenanceRepository,
                              TankRepository tankRepository,
                              @Value("${app.business-zone:Asia/Shanghai}") String businessZone) {
        this.maintenanceRepository = maintenanceRepository;
        this.tankRepository = tankRepository;
        try {
            this.businessZone = ZoneId.of(businessZone);
        } catch (DateTimeException ex) {
            throw new IllegalStateException("无效的业务时区配置: " + businessZone, ex);
        }
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
        MaintenanceResult result = parseResult(resultLabel);
        String trimmedNote = note == null ? null : note.trim();
        if (result == MaintenanceResult.ABNORMAL
                && (trimmedNote == null || trimmedNote.isEmpty())) {
            throw new BizException("异常巡检必须填写非空备注，请说明异常情况");
        }

        // 字段本身不合法时先报字段错误；是否重复在业务规则之后再判断
        LocalDate today = LocalDate.now(Clock.system(businessZone));
        if (checkDate.isAfter(today)) {
            throw new BizException("巡检日期不能晚于今天（" + today + "），不允许登记未来日期");
        }

        // 应用层先给出明确提示；真正的并发兜底仍由数据库唯一约束负责。
        if (maintenanceRepository.existsByTankIdAndCheckDate(tankId, checkDate)) {
            throw new ConflictException("展缸 " + tank.getCode() + " 在 " + checkDate
                    + " 已有一条巡检记录，同一展缸同一天只能登记一次");
        }

        Maintenance m = new Maintenance();
        m.setTankId(tankId);
        m.setCheckDate(checkDate);
        m.setResult(result);
        m.setNote(trimmedNote);
        try {
            return maintenanceRepository.saveAndFlush(m);
        } catch (DataIntegrityViolationException ex) {
            // 两名饲养员并发提交同一口缸同一天：一笔成功，另一笔在此被唯一索引拦下。
            log.warn("巡检登记命中唯一约束 tankId={}, date={}", tankId, checkDate);
            throw new ConflictException("展缸 " + tank.getCode() + " 在 " + checkDate
                    + " 的巡检记录刚刚已被其他同事登记，请勿重复提交");
        }
    }

    private MaintenanceResult parseResult(String label) {
        String trimmed = label.trim();
        if ("异常".equals(trimmed) || MaintenanceResult.ABNORMAL.name().equals(trimmed)) {
            return MaintenanceResult.ABNORMAL;
        }
        if ("正常".equals(trimmed) || MaintenanceResult.NORMAL.name().equals(trimmed)) {
            return MaintenanceResult.NORMAL;
        }
        throw new BizException("巡检结果只能是「正常」或「异常」");
    }
}
