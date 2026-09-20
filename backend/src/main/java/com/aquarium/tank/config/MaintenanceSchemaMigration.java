package com.aquarium.tank.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 旧库迁移：为 maintenance 补 (tank_id, check_date) 唯一索引。
 *
 * 新装库的 schema.sql 已直接包含该唯一索引；对升级前已运行过、表中可能存在
 * 历史重复记录的旧库，本迁移器先执行去重脚本再建索引，保证迁移不会因为
 * 存量脏数据而失败。整段过程幂等，可重复启动。
 */
@Component
public class MaintenanceSchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceSchemaMigration.class);
    private static final String INDEX_NAME = "uk_maintenance_tank_date";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public MaintenanceSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void migrate() {
        String database;
        try (Connection conn = dataSource.getConnection()) {
            database = conn.getCatalog();
        } catch (Exception e) {
            throw new IllegalStateException("无法读取数据库名，巡检唯一索引迁移中止", e);
        }
        if (database == null || database.isBlank()) {
            throw new IllegalStateException("未选择数据库，巡检唯一索引迁移中止");
        }

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT 1 FROM information_schema.statistics " +
                        "WHERE table_schema = ? AND table_name = 'maintenance' AND index_name = ?",
                database, INDEX_NAME);
        if (!existing.isEmpty()) {
            return;
        }

        log.warn("maintenance 缺少唯一索引 {}，开始合并历史重复记录并补建索引……", INDEX_NAME);
        EncodedResource script = new EncodedResource(
                new ClassPathResource("db/migration/V2__maintenance_unique.sql"),
                StandardCharsets.UTF_8);

        // 去重语句逐条自动提交，避免 ALTER TABLE 在部分数据库上隐式回滚前置 DML
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            ScriptUtils.executeSqlScript(conn, script);
            log.info("巡检唯一索引 {} 迁移完成", INDEX_NAME);
        } catch (Exception e) {
            throw new IllegalStateException("巡检唯一索引迁移失败：" + e.getMessage(), e);
        }
    }
}
