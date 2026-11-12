-- =============================================================
-- 校园报修预约系统 增量升级脚本：维修工角色
-- 用途：给已有数据库升级使用（不重建表，历史工单与图片完整保留）
-- 适用：已完成 sql/schema.sql（或历史升级脚本）初始化的 MySQL 8.0+ 数据库
-- 内容：
--   1) repair_order 新增 worker_id / rating_score / rating_comment / rating_time 列
--   2) 新增维修工档案表 repair_worker
--   3) 新增维修工-擅长分类关联表 repair_worker_category
-- 说明：脚本可重复执行；列/索引已存在时自动跳过；建表使用 IF NOT EXISTS
-- 执行方式：mysql --default-character-set=utf8mb4 -u root -p -e "source sql/upgrade_worker.sql"
-- 提示：脚本已 SET NAMES utf8mb4，写入数据不会乱码；若控制台输出中文显示为乱码，
--       先执行 chcp 65001 切到 UTF-8 代码页即可，与数据库中的数据无关。
-- =============================================================

USE campus_repair;

-- 统一会话排序规则，避免与 utf8mb4_general_ci 字段混用时报 1271
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 1) repair_order 新增 worker_id（维修工用户ID）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'worker_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN worker_id BIGINT DEFAULT NULL COMMENT ''维修工用户ID（关联sys_user）''',
    'SET @skipped_worker_id = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.1) 维修工检索索引（列已存在但索引缺失时补齐）
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND INDEX_NAME = 'idx_worker_id'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE repair_order ADD INDEX idx_worker_id (worker_id)',
    'SET @skipped_worker_id_idx = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2) repair_order 新增 rating_score（评分1-5星）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'rating_score'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN rating_score TINYINT DEFAULT NULL COMMENT ''评分（1-5星，完成后由报修人评价）''',
    'SET @skipped_rating_score = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.3) repair_order 新增 rating_comment（评价内容）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'rating_comment'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN rating_comment VARCHAR(200) DEFAULT NULL COMMENT ''评价内容''',
    'SET @skipped_rating_comment = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.4) repair_order 新增 rating_time（评价时间）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'rating_time'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN rating_time DATETIME DEFAULT NULL COMMENT ''评价时间''',
    'SET @skipped_rating_time = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 维修工档案表
CREATE TABLE IF NOT EXISTS repair_worker (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT       NOT NULL COMMENT '维修工用户ID（关联sys_user）',
    intro        VARCHAR(200) DEFAULT NULL COMMENT '维修工简介',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在职 0停用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT '维修工档案表';

-- 3) 维修工-擅长分类关联表
CREATE TABLE IF NOT EXISTS repair_worker_category (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    worker_id   BIGINT NOT NULL COMMENT '维修工档案ID（关联repair_worker）',
    category_id BIGINT NOT NULL COMMENT '故障分类ID（关联fault_category）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_worker_category (worker_id, category_id),
    INDEX idx_category_id (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT '维修工-擅长分类关联表';
