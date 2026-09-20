-- 维生巡检：同一展缸同一巡检日期唯一
-- 仅在旧库缺少唯一索引时由 MaintenanceSchemaMigration 执行（语句需保持幂等）。
-- 历史台账中可能已存在重复登记，直接建唯一索引会使迁移失败，因此先安全去重：
--   1) 把同组其他记录的非空备注合并到将保留的记录上，异常说明不丢失；
--   2) 每组优先保留异常记录，其次保留最新（id 最大）的一条，删除其余重复行；
--   3) 最后补建唯一索引，作为并发提交的最终兜底。

UPDATE maintenance m
JOIN (
    SELECT COALESCE(MAX(CASE WHEN result = 'ABNORMAL' THEN id END), MAX(id)) AS keep_id,
           tank_id, check_date
    FROM maintenance
    GROUP BY tank_id, check_date
    HAVING COUNT(*) > 1
) k ON m.tank_id = k.tank_id AND m.check_date = k.check_date AND m.id = k.keep_id
JOIN (
    -- 只聚合被删除行的非空备注，保留行自带备注不重复拼接
    SELECT x.tank_id, x.check_date,
           SUBSTRING(GROUP_CONCAT(x.note ORDER BY x.id SEPARATOR '；'), 1, 255) AS other_notes
    FROM maintenance x
    JOIN (
        SELECT COALESCE(MAX(CASE WHEN result = 'ABNORMAL' THEN id END), MAX(id)) AS keep_id,
               tank_id, check_date
        FROM maintenance
        GROUP BY tank_id, check_date
        HAVING COUNT(*) > 1
    ) kx ON kx.tank_id = x.tank_id AND kx.check_date = x.check_date AND x.id <> kx.keep_id
    WHERE x.note IS NOT NULL AND x.note <> ''
    GROUP BY x.tank_id, x.check_date
) n ON n.tank_id = m.tank_id AND n.check_date = m.check_date
SET m.note = CASE
    WHEN m.note IS NULL OR m.note = '' THEN n.other_notes
    ELSE SUBSTRING(CONCAT(m.note, '；', n.other_notes), 1, 255)
END;

DELETE FROM maintenance
WHERE id NOT IN (
    SELECT keep_id FROM (
        SELECT COALESCE(MAX(CASE WHEN result = 'ABNORMAL' THEN id END), MAX(id)) AS keep_id
        FROM maintenance
        GROUP BY tank_id, check_date
    ) kept
);

ALTER TABLE maintenance
    ADD CONSTRAINT uk_maintenance_tank_date UNIQUE KEY (tank_id, check_date);
