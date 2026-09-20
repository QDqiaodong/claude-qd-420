package com.aquarium.tank.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 历史库兼容迁移：为 maintenance(tank_id, check_date) 补建唯一索引。
 *
 * 旧版本没有唯一约束，台账中可能已经存在同一展缸同一天的重复记录。
 * 直接 CREATE UNIQUE INDEX 会让旧库迁移失败、应用无法启动，因此先按
 * 「异常记录优先、其次 id 最小」的规则合并去重，再补建索引。
 * 整个过程通过 information_schema 判断，重复执行不会产生副作用。
 */
@Component
public class MaintenanceSchemaMigration implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceSchemaMigration.class);
    private static final String UNIQUE_INDEX = "uk_maintenance_tank_date";

    private final JdbcTemplate jdbc;

    public MaintenanceSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterSingletonsInstantiated() {
        dedupeLegacyRows();
        ensureUniqueIndex();
    }

    private void dedupeLegacyRows() {
        Integer duplicatedGroups = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" +
                "  SELECT 1 FROM maintenance" +
                "  GROUP BY tank_id, check_date HAVING COUNT(*) > 1" +
                ") t", Integer.class);
        if (duplicatedGroups == null || duplicatedGroups == 0) {
            return;
        }

        // 每个 (tank_id, check_date) 分组保留一条：
        //   1) 优先保留 ABNORMAL（异常记录带有排查信息，不能丢）；
        //   2) 同为正常或同为异常时，保留最早登记的一条（id 最小）。
        int deleted = jdbc.update(
                "DELETE FROM maintenance WHERE id NOT IN (" +
                "  SELECT keep_id FROM (" +
                "    SELECT MIN(id) AS keep_id FROM maintenance m1" +
                "    WHERE NOT EXISTS (" +
                "      SELECT 1 FROM maintenance m2" +
                "      WHERE m2.tank_id = m1.tank_id AND m2.check_date = m1.check_date" +
                "        AND (m2.result = 'ABNORMAL' AND m1.result <> 'ABNORMAL'" +
                "             OR (m2.result = m1.result AND m2.id < m1.id))" +
                "    )" +
                "    GROUP BY m1.tank_id, m1.check_date" +
                "  ) kept" +
                ")");
        log.warn("maintenance 存在历史重复巡检记录，已按异常优先/最早登记规则合并删除 {} 条冗余记录", deleted);
    }

    private void ensureUniqueIndex() {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics" +
                " WHERE table_schema = DATABASE()" +
                "   AND table_name = 'maintenance'" +
                "   AND index_name = ?", Integer.class, UNIQUE_INDEX);
        if (exists != null && exists > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE maintenance"
                + " ADD UNIQUE KEY " + UNIQUE_INDEX + " (tank_id, check_date)");
        log.info("已为 maintenance(tank_id, check_date) 补建唯一索引 {}", UNIQUE_INDEX);
    }
}
