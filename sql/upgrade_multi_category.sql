-- =============================================================
-- 校园报修预约系统 增量升级脚本：工单分类改为「按工种、可多选」
-- 用途：给已有数据库升级使用（不重建表，历史工单与图片完整保留）
-- 适用：已完成 sql/schema.sql 初始化的 MySQL 8.0+ 数据库
-- 内容：
--   1) 新增工单-分类关联表 repair_order_category（支持多选）
--   2) 把历史工单的 category_id 回填到关联表
--   3) fault_category 去掉 location_type（分类不再区分宿舍/公共区域）
--   4) 删除旧的 12 条预设分类，写入新的 11 条工种分类
-- 说明：脚本可重复执行；已删除的列、已写入的分类会自动跳过
-- 执行方式：mysql --default-character-set=utf8mb4 -u root -p -e "source sql/upgrade_multi_category.sql"
-- 提示：脚本已 SET NAMES utf8mb4，写入数据不会乱码；若控制台输出中文显示为乱码，
--       先执行 chcp 65001 切到 UTF-8 代码页即可，与数据库中的数据无关。
-- =============================================================

USE campus_repair;

-- 统一会话排序规则：
-- 表字段为 utf8mb4_general_ci，而 MySQL 8 默认连接排序规则是 utf8mb4_0900_ai_ci，
-- 两者混用会让 name IN (...) 报 1271 Illegal mix of collations。
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 1) 工单-分类关联表
CREATE TABLE IF NOT EXISTS repair_order_category (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id    BIGINT NOT NULL COMMENT '工单ID（关联repair_order）',
    category_id BIGINT NOT NULL COMMENT '故障分类ID（关联fault_category）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_category (order_id, category_id),
    INDEX idx_category_id (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT '工单-故障分类关联表（多选）';

-- 2) 回填历史工单的分类关联（重复执行不会产生重复行）
INSERT IGNORE INTO repair_order_category (order_id, category_id)
SELECT id, category_id FROM repair_order WHERE category_id IS NOT NULL;

-- 3) 分类不再区分地点（列已删除时自动跳过，保证脚本可重复执行）
-- 注意：跳过时用 SET 而非中文 SELECT，避免 Windows 控制台以 GBK 渲染 UTF-8 输出造成乱码
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fault_category'
      AND COLUMN_NAME = 'location_type'
);
SET @ddl = IF(@col_exists > 0,
    'ALTER TABLE fault_category DROP COLUMN location_type',
    'SET @skipped_location_type = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 删除旧的 12 条预设分类（项目未投入使用，无需保留停用数据）
--    顺序：先删工单-分类关联，再删分类，最后清理工单上失效的主分类ID
DELETE oc FROM repair_order_category oc
JOIN fault_category c ON c.id = oc.category_id
WHERE c.name IN ('水电故障', '门窗损坏', '空调故障', '卫浴马桶堵塞', '灯具损坏', '柜子床架损坏',
                 '井盖破损', '标识牌损坏', '路面瓷砖破损', '路灯故障', '绿化设施损坏', '宣传栏损坏');

DELETE FROM fault_category
WHERE name IN ('水电故障', '门窗损坏', '空调故障', '卫浴马桶堵塞', '灯具损坏', '柜子床架损坏',
               '井盖破损', '标识牌损坏', '路面瓷砖破损', '路灯故障', '绿化设施损坏', '宣传栏损坏');

-- 历史工单若引用了已删除的分类，置空主分类字段（前端会回退显示「其他故障」）
UPDATE repair_order
SET category_id = NULL
WHERE category_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM fault_category c WHERE c.id = repair_order.category_id);

-- 5) 写入新的 11 条工种分类（已存在同名分类时跳过）

INSERT INTO fault_category (name, sort)
SELECT t.name, t.sort FROM (
    SELECT '水电类' AS name, 1 AS sort UNION ALL
    SELECT '木五金类', 2 UNION ALL
    SELECT '土建修缮', 3 UNION ALL
    SELECT '空调电器', 4 UNION ALL
    SELECT '网络弱电', 5 UNION ALL
    SELECT '公共设施', 6 UNION ALL
    SELECT '家用电子数码', 7 UNION ALL
    SELECT '交通工具维修', 8 UNION ALL
    SELECT '家装&管道维修', 9 UNION ALL
    SELECT '工业/机房运维', 10 UNION ALL
    SELECT '其他小众维修', 11
) t
WHERE NOT EXISTS (SELECT 1 FROM fault_category c WHERE c.name = t.name);
