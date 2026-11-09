-- =============================================================
-- 校园报修预约系统 数据库建表脚本
-- 数据库：MySQL 8.0+，字符集 utf8mb4
-- 说明：一套数据库同时支撑 微信小程序端 + Vue 网页管理后台
-- =============================================================

CREATE DATABASE IF NOT EXISTS campus_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE campus_repair;

-- -------------------------------------------------------------
-- 用户表：普通用户 / 会员用户 / 维修管理员 共用一张表
-- 角色字段 role 区分身份，小程序与网页端登录同一账号权限一致
-- -------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    phone        VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号（登录账号）',
    nickname     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER普通用户 VIP会员用户 ADMIN维修管理员',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB COMMENT '系统用户表';

-- 预置一个管理员账号，手机号 13800000000，验证码登录即可
INSERT INTO sys_user (phone, nickname, role) VALUES ('13800000000', '维修管理员', 'ADMIN');

-- -------------------------------------------------------------
-- 短信验证码表：课程作业采用模拟短信（验证码打印到后端日志）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS sms_code;
CREATE TABLE sms_code (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    phone        VARCHAR(20)  NOT NULL COMMENT '手机号',
    code         VARCHAR(10)  NOT NULL COMMENT '验证码',
    expire_time  DATETIME     NOT NULL COMMENT '过期时间',
    used         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已使用：1是 0否',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone (phone)
) ENGINE = InnoDB COMMENT '短信验证码表';

-- -------------------------------------------------------------
-- 故障分类表：管理员可在后台/小程序动态维护（增删改），非写死
-- location_type 区分分类归属：DORM宿舍楼内 PUBLIC校园公共区域
-- -------------------------------------------------------------
DROP TABLE IF EXISTS fault_category;
CREATE TABLE fault_category (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(50)  NOT NULL COMMENT '分类名称',
    location_type VARCHAR(20)  NOT NULL COMMENT '归属地点类型：DORM宿舍楼内 PUBLIC校园公共区域',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB COMMENT '故障分类表（管理员可维护）';

-- 初始预设分类
INSERT INTO fault_category (name, location_type, sort) VALUES
('水电故障',     'DORM', 1),
('门窗损坏',     'DORM', 2),
('空调故障',     'DORM', 3),
('卫浴马桶堵塞', 'DORM', 4),
('灯具损坏',     'DORM', 5),
('柜子床架损坏', 'DORM', 6),
('井盖破损',     'PUBLIC', 1),
('标识牌损坏',   'PUBLIC', 2),
('路面瓷砖破损', 'PUBLIC', 3),
('路灯故障',     'PUBLIC', 4),
('绿化设施损坏', 'PUBLIC', 5),
('宣传栏损坏',   'PUBLIC', 6);

-- -------------------------------------------------------------
-- 报修工单表
-- 状态流转：PENDING待接单 -> IN_PROGRESS维修中 -> FINISHED已完成
--           PENDING/IN_PROGRESS -> REJECTED驳回
-- -------------------------------------------------------------
DROP TABLE IF EXISTS repair_order;
CREATE TABLE repair_order (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no         VARCHAR(32)  NOT NULL UNIQUE COMMENT '工单编号',
    user_id          BIGINT       NOT NULL COMMENT '报修人ID（关联sys_user）',
    location_type    VARCHAR(20)  NOT NULL COMMENT '地点类型：DORM宿舍楼内 PUBLIC校园公共区域',
    building_no      VARCHAR(50)  DEFAULT NULL COMMENT '宿舍楼号（location_type=DORM时填写）',
    room_no          VARCHAR(50)  DEFAULT NULL COMMENT '宿舍房间号（location_type=DORM时填写）',
    public_location  VARCHAR(200) DEFAULT NULL COMMENT '公共区域具体位置（location_type=PUBLIC时填写）',
    category_id      BIGINT       NOT NULL COMMENT '故障分类ID（关联fault_category）',
    description      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '故障文字描述',
    urgent           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否紧急：1紧急 0普通（仅会员可标记）',
    appointment_time DATETIME     DEFAULT NULL COMMENT '会员自选预约上门时间（普通用户为NULL，系统默认派单）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '工单状态：PENDING待接单 IN_PROGRESS维修中 FINISHED已完成 REJECTED驳回',
    admin_remark     VARCHAR(500) DEFAULT NULL COMMENT '维修备注（管理员填写）',
    admin_id         BIGINT       DEFAULT NULL COMMENT '接单管理员ID',
    accept_time      DATETIME     DEFAULT NULL COMMENT '接单时间',
    finish_time      DATETIME     DEFAULT NULL COMMENT '完成时间',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_urgent (urgent),
    INDEX idx_category_id (category_id)
) ENGINE = InnoDB COMMENT '报修工单表';

-- -------------------------------------------------------------
-- 工单故障图片表：一个工单可有多张图片
-- 普通用户最多1张，会员最多5张（后端校验）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS repair_order_image;
CREATE TABLE repair_order_image (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id     BIGINT       NOT NULL COMMENT '工单ID（关联repair_order）',
    image_url    VARCHAR(300) NOT NULL COMMENT '图片访问URL（本地存储相对路径）',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '图片顺序',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_order_id (order_id)
) ENGINE = InnoDB COMMENT '工单故障图片表';
