CREATE TABLE IF NOT EXISTS tank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    state VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS creature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tank_id BIGINT NOT NULL,
    species VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_creature_tank FOREIGN KEY (tank_id) REFERENCES tank(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feeding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tank_id BIGINT NOT NULL,
    feed_date DATE NOT NULL,
    food VARCHAR(64) NOT NULL,
    qty INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tank_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    result VARCHAR(16) NOT NULL,
    note VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO tank (id, code, capacity, state) VALUES
 (1, 'T01', 4, 'NORMAL'),
 (2, 'T02', 2, 'MAINTENANCE'),
 (3, 'T03', 3, 'DISABLED'),
 (4, 'T04', 5, 'NORMAL');

INSERT IGNORE INTO creature (id, tank_id, species, name, status) VALUES
 (1, 1, '小丑鱼', '番茄', 'ON_DISPLAY'),
 (2, 1, '蓝吊', '多莉', 'ON_DISPLAY'),
 (3, 2, '绿海龟', '老绿', 'ON_DISPLAY'),
 (4, 4, '海月水母', '月儿', 'ON_DISPLAY'),
 (5, 4, '倒立水母', '泡泡', 'OFF');

INSERT IGNORE INTO feeding (id, tank_id, feed_date, food, qty) VALUES
 (1, 1, '2026-09-15', '丰年虾', 20),
 (2, 1, '2026-09-16', '颗粒饲料', 15),
 (3, 2, '2026-09-16', '蔬菜', 10),
 (4, 4, '2026-09-17', '卤虫', 8);

INSERT IGNORE INTO maintenance (id, tank_id, check_date, result, note) VALUES
 (1, 1, '2026-09-15', 'NORMAL', NULL),
 (2, 1, '2026-09-16', 'ABNORMAL', '蛋白分离器起泡异常'),
 (3, 4, '2026-09-17', 'NORMAL', NULL),
 (4, 3, '2026-09-14', 'NORMAL', '停用前巡检');
