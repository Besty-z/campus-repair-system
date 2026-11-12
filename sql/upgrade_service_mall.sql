-- =============================================================
-- 校园报修预约系统 增量升级脚本：服务价格 + 首页广告位
-- 用途：给已有数据库升级使用（不重建表，历史工单与图片完整保留）
-- 适用：已完成 sql/schema.sql（或历史升级脚本）初始化的 MySQL 8.0+ 数据库
-- 内容：
--   1) fault_category 新增 price（基础服务价，元）/ image_url（服务图片）列
--   2) repair_order 新增 estimated_price（预估费用，会员已按 8 折计算）列
--   3) 新增首页广告位表 ad_banner
--   4) 同步 11 个预设分类的价格与服务图片
--   5) 插入 3 条首页广告（按 title 判重，已存在则跳过）
-- 说明：脚本可重复执行；列已存在时自动跳过；建表使用 IF NOT EXISTS；
--       分类数据使用 UPDATE ... WHERE name 幂等同步，广告按 title 判重插入。
-- 执行方式：mysql --default-character-set=utf8mb4 -u root -p -e "source sql/upgrade_service_mall.sql"
-- 提示：脚本已 SET NAMES utf8mb4，写入数据不会乱码；若控制台输出中文显示为乱码，
--       先执行 chcp 65001 切到 UTF-8 代码页即可，与数据库中的数据无关。
-- =============================================================

USE campus_repair;

-- 统一会话排序规则，避免与 utf8mb4_general_ci 字段混用时报 1271
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 1) fault_category 新增 price（基础服务价，元）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fault_category'
      AND COLUMN_NAME = 'price'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE fault_category ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT ''基础服务价（元）'' AFTER name',
    'SET @skipped_category_price = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.1) fault_category 新增 image_url（服务图片）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fault_category'
      AND COLUMN_NAME = 'image_url'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE fault_category ADD COLUMN image_url VARCHAR(512) DEFAULT NULL COMMENT ''服务图片URL'' AFTER price',
    'SET @skipped_category_image_url = 1');
-- 图片 URL 较长（含生成参数），统一放宽到 512 避免 1406 Data too long
ALTER TABLE fault_category MODIFY COLUMN image_url VARCHAR(512) DEFAULT NULL COMMENT '服务图片URL';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) repair_order 新增 estimated_price（预估费用）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'repair_order'
      AND COLUMN_NAME = 'estimated_price'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE repair_order ADD COLUMN estimated_price DECIMAL(10,2) DEFAULT NULL COMMENT ''预估费用（元，会员已按8折计算；选择“其他”时为NULL表示面议）'' AFTER custom_category',
    'SET @skipped_order_estimated_price = 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 首页广告位表
CREATE TABLE IF NOT EXISTS ad_banner (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title        VARCHAR(50)  NOT NULL COMMENT '广告标题',
    subtitle     VARCHAR(100) DEFAULT NULL COMMENT '广告副标题',
    image_url    VARCHAR(512) NOT NULL COMMENT '广告图片URL',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT '首页广告位表';

-- 兼容早期建表：图片 URL 长度统一放宽到 512，避免广告图片地址过长写入失败
ALTER TABLE ad_banner MODIFY COLUMN image_url VARCHAR(512) NOT NULL COMMENT '广告图片URL';

-- 4) 同步预设分类的价格与服务图片（可重复执行）
UPDATE fault_category SET price = 80.00, image_url = 'http://localhost:8080/api/uploads/samples/service/water_1.jpg' WHERE name = '水电类';
UPDATE fault_category SET price = 70.00, image_url = 'http://localhost:8080/api/uploads/samples/service/wood_1.jpg' WHERE name = '木五金类';
UPDATE fault_category SET price = 120.00, image_url = 'http://localhost:8080/api/uploads/samples/service/civil_1.jpg' WHERE name = '土建修缮';
UPDATE fault_category SET price = 100.00, image_url = 'http://localhost:8080/api/uploads/samples/service/ac_1.jpg' WHERE name = '空调电器';
UPDATE fault_category SET price = 90.00, image_url = 'http://localhost:8080/api/uploads/samples/service/network_1.jpg' WHERE name = '网络弱电';
UPDATE fault_category SET price = 110.00, image_url = 'http://localhost:8080/api/uploads/samples/service/public_1.jpg' WHERE name = '公共设施';
UPDATE fault_category SET price = 90.00, image_url = 'http://localhost:8080/api/uploads/samples/service/digital_1.jpg' WHERE name = '家用电子数码';
UPDATE fault_category SET price = 60.00, image_url = 'http://localhost:8080/api/uploads/samples/service/vehicle_1.jpg' WHERE name = '交通工具维修';
UPDATE fault_category SET price = 130.00, image_url = 'http://localhost:8080/api/uploads/samples/service/renovation_1.jpg' WHERE name = '家装&管道维修';
UPDATE fault_category SET price = 150.00, image_url = 'http://localhost:8080/api/uploads/samples/service/idc_1.jpg' WHERE name = '工业/机房运维';
UPDATE fault_category SET price = 70.00, image_url = 'http://localhost:8080/api/uploads/samples/service/misc_1.jpg' WHERE name = '其他小众维修';

-- 5) 插入首页广告（按 title 判重，已存在则跳过）
SET @ad_exists = (SELECT COUNT(*) FROM ad_banner WHERE title = '校园上门维修，1 小时响应');
INSERT INTO ad_banner (title, subtitle, image_url, sort, status)
SELECT '校园上门维修，1 小时响应', '宿舍与公共区域全覆盖，专业师傅上门',
       'http://localhost:8080/api/uploads/samples/ads/ad-campus_1.jpg',
       1, 1
FROM DUAL
WHERE @ad_exists = 0;

SET @ad_exists = (SELECT COUNT(*) FROM ad_banner WHERE title = '夜间紧急报修，师傅随时待命');
INSERT INTO ad_banner (title, subtitle, image_url, sort, status)
SELECT '夜间紧急报修，师傅随时待命', '水管爆裂、跳闸断电，优先安排处理',
       'http://localhost:8080/api/uploads/samples/ads/ad-night_1.jpg',
       2, 1
FROM DUAL
WHERE @ad_exists = 0;

SET @ad_exists = (SELECT COUNT(*) FROM ad_banner WHERE title = '开通会员，全部服务 8 折');
INSERT INTO ad_banner (title, subtitle, image_url, sort, status)
SELECT '开通会员，全部服务 8 折', '最多 5 张图 · 预约上门 · 紧急优先',
       'http://localhost:8080/api/uploads/samples/ads/ad-vip_1.jpg',
       3, 1
FROM DUAL
WHERE @ad_exists = 0;

-- 6) 已存在广告的历史数据同步为内置样板图（可重复执行）
UPDATE ad_banner SET image_url = 'http://localhost:8080/api/uploads/samples/ads/ad-campus_1.jpg'
WHERE title = '校园上门维修，1 小时响应';
UPDATE ad_banner SET image_url = 'http://localhost:8080/api/uploads/samples/ads/ad-night_1.jpg'
WHERE title = '夜间紧急报修，师傅随时待命';
UPDATE ad_banner SET image_url = 'http://localhost:8080/api/uploads/samples/ads/ad-vip_1.jpg'
WHERE title = '开通会员，全部服务 8 折';
