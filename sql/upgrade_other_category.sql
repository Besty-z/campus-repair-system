-- =============================================================
-- 校园报修预约系统 增量升级脚本：故障分类新增「其他（自行填写）」
-- 用途：给已有数据库升级使用（不删除、不重建表，原有数据完整保留）
-- 适用：已完成 sql/schema.sql 初始化的 MySQL 8.0+ 数据库
-- 内容：
--   1) repair_order.category_id 改为允许 NULL（选择“其他”时无分类ID）
--   2) repair_order 新增 custom_category 字段，记录用户自行填写的故障类型
-- 执行方式：mysql -u root -p < upgrade_other_category.sql
-- =============================================================

USE campus_repair;

-- 分类ID允许为空：用户选择“其他（自行填写）”时不关联具体分类
ALTER TABLE repair_order MODIFY COLUMN category_id BIGINT DEFAULT NULL COMMENT '故障分类ID（选择其他自定义类型时为NULL）';

-- 自定义故障类型：紧跟 category_id 之后，便于查看
ALTER TABLE repair_order ADD COLUMN custom_category VARCHAR(50) DEFAULT NULL COMMENT '自定义故障类型（用户选择“其他”时填写）' AFTER category_id;
