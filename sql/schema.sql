-- =============================================================
-- 校园报修预约系统 数据库建表脚本
-- 数据库：MySQL 8.0+，字符集 utf8mb4
-- 说明：一套数据库同时支撑 微信小程序端 + Vue 网页管理后台
-- =============================================================

CREATE DATABASE IF NOT EXISTS campus_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE campus_repair;

-- 显式声明客户端字符集，避免 Windows 下客户端按 GBK/latin1 解析本文件中的中文
SET NAMES utf8mb4;

-- -------------------------------------------------------------
-- 用户表：普通用户 / 会员用户 / 维修管理员 共用一张表
-- 角色字段 role 区分身份，小程序与网页端登录同一账号权限一致
-- -------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    phone        VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号（登录账号）',
    nickname     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER普通用户 VIP会员用户 ADMIN维修管理员 WORKER维修工',
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
-- 故障分类表：按维修工种划分，不区分报修地点，管理员可动态维护
-- -------------------------------------------------------------
DROP TABLE IF EXISTS fault_category;
CREATE TABLE fault_category (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(50)   NOT NULL COMMENT '分类名称',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '基础服务价（元）',
    image_url     VARCHAR(512)  DEFAULT NULL COMMENT '服务图片URL',
    sort          INT           NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB COMMENT '故障分类表（管理员可维护）';

-- 初始预设分类（「其他」由前端以“其他（自行填写）”选项呈现，需填写自定义故障类型）
-- price 为基础服务价（元）；image_url 为分类服务图片，用于用户端展示
INSERT INTO fault_category (name, price, image_url, sort) VALUES
('水电类',       80.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=plumber%20repairing%20water%20pipe%20and%20light%20switch%20in%20dormitory%2C%20realistic%20photo%2C%20tools%2C%20bright%20natural%20light&image_size=landscape_4_3', 1),
('木五金类',     70.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=carpenter%20repairing%20wooden%20door%20lock%20and%20bunk%20bed%20in%20dormitory%2C%20realistic%20photo&image_size=landscape_4_3', 2),
('土建修缮',    120.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=worker%20repairing%20cracked%20wall%20and%20loose%20floor%20tiles%20indoors%2C%20realistic%20photo&image_size=landscape_4_3', 3),
('空调电器',    100.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=technician%20cleaning%20and%20repairing%20wall%20mounted%20air%20conditioner%2C%20realistic%20photo&image_size=landscape_4_3', 4),
('网络弱电',     90.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=network%20technician%20installing%20ethernet%20cable%20and%20wifi%20access%20point%20in%20dorm%20room%2C%20realistic%20photo&image_size=landscape_4_3', 5),
('公共设施',    110.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=worker%20repairing%20street%20lamp%20and%20manhole%20cover%20on%20campus%20road%2C%20realistic%20photo&image_size=landscape_4_3', 6),
('家用电子数码', 90.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=technician%20repairing%20laptop%20and%20smartphone%20at%20repair%20desk%2C%20realistic%20photo&image_size=landscape_4_3', 7),
('交通工具维修', 60.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=mechanic%20repairing%20electric%20bicycle%20in%20campus%20workshop%2C%20realistic%20photo&image_size=landscape_4_3', 8),
('家装&管道维修', 130.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=handyman%20repairing%20bathroom%20plumbing%20and%20window%20screen%2C%20realistic%20photo&image_size=landscape_4_3', 9),
('工业/机房运维', 150.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=IT%20engineer%20maintaining%20server%20rack%20and%20network%20switch%20in%20data%20center%2C%20realistic%20photo&image_size=landscape_4_3', 10),
('其他小众维修', 70.00, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=craftsman%20repairing%20leather%20shoes%20and%20watch%20at%20workbench%2C%20realistic%20photo&image_size=landscape_4_3', 11);

-- -------------------------------------------------------------
-- 首页广告位表：用户端首页轮播展示，管理员可维护
-- -------------------------------------------------------------
DROP TABLE IF EXISTS ad_banner;
CREATE TABLE ad_banner (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title        VARCHAR(50)  NOT NULL COMMENT '广告标题',
    subtitle     VARCHAR(100) DEFAULT NULL COMMENT '广告副标题',
    image_url    VARCHAR(512) NOT NULL COMMENT '广告图片URL',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB COMMENT '首页广告位表';

-- 初始广告位（首页轮播，sort 越小越靠前）
INSERT INTO ad_banner (title, subtitle, image_url, sort, status) VALUES
('校园上门维修，1 小时响应', '宿舍与公共区域全覆盖，专业师傅上门', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Campus%20repair%20service%20banner%2C%20friendly%20technician%20holding%20toolbox%2C%20modern%20university%20dormitory%2C%20blue%20gradient%20background%2C%20clean%20professional%20photo%2C%20wide%20composition&image_size=landscape_16_9', 1, 1),
('夜间紧急报修，师傅随时待命', '水管爆裂、跳闸断电，优先安排处理', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=24%20hour%20emergency%20repair%20banner%2C%20electrician%20working%20at%20night%20with%20headlamp%2C%20campus%20building%2C%20dramatic%20blue%20lighting%2C%20professional%20photo%2C%20wide%20composition&image_size=landscape_16_9', 2, 1),
('开通会员，全部服务 8 折', '最多 5 张图 · 预约上门 · 紧急优先', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=VIP%20membership%20promotion%20banner%2C%20premium%20home%20repair%20service%2C%20golden%20and%20blue%20gradient%2C%20technician%20and%20student%20handshake%2C%20clean%20modern%20photo%2C%20wide%20composition&image_size=landscape_16_9', 3, 1);

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
    category_id      BIGINT       DEFAULT NULL COMMENT '主分类ID（多分类中的第一个；选择“其他”时为NULL）',
    custom_category  VARCHAR(50)  DEFAULT NULL COMMENT '自定义故障类型（用户选择“其他”时填写）',
    estimated_price  DECIMAL(10,2) DEFAULT NULL COMMENT '预估费用（元，会员已按8折计算；选择“其他”时为NULL表示面议）',
    description      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '故障文字描述',
    urgent           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否紧急：1紧急 0普通（仅会员可标记）',
    appointment_time DATETIME     DEFAULT NULL COMMENT '会员自选预约上门时间（普通用户为NULL，系统默认派单）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '工单状态：PENDING待接单 IN_PROGRESS维修中 FINISHED已完成 REJECTED驳回',
    admin_remark     VARCHAR(500) DEFAULT NULL COMMENT '维修备注（管理员/维修工填写）',
    admin_id         BIGINT       DEFAULT NULL COMMENT '接单管理员ID',
    worker_id        BIGINT       DEFAULT NULL COMMENT '维修工用户ID（关联sys_user）',
    accept_time      DATETIME     DEFAULT NULL COMMENT '接单时间',
    finish_time      DATETIME     DEFAULT NULL COMMENT '完成时间',
    rating_score     TINYINT      DEFAULT NULL COMMENT '评分（1-5星，完成后由报修人评价）',
    rating_comment   VARCHAR(200) DEFAULT NULL COMMENT '评价内容',
    rating_time      DATETIME     DEFAULT NULL COMMENT '评价时间',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_urgent (urgent),
    INDEX idx_category_id (category_id),
    INDEX idx_worker_id (worker_id)
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

-- -------------------------------------------------------------
-- 工单-分类关联表：一个工单可选择多个故障分类（多选）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS repair_order_category;
CREATE TABLE repair_order_category (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id    BIGINT NOT NULL COMMENT '工单ID（关联repair_order）',
    category_id BIGINT NOT NULL COMMENT '故障分类ID（关联fault_category）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_category (order_id, category_id),
    INDEX idx_category_id (category_id)
) ENGINE = InnoDB COMMENT '工单-故障分类关联表（多选）';

-- -------------------------------------------------------------
-- 维修工档案表：每个维修工对应一个系统用户账号
-- 角色为 WORKER 的用户在此维护简介、状态与擅长分类
-- -------------------------------------------------------------
DROP TABLE IF EXISTS repair_worker;
CREATE TABLE repair_worker (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT       NOT NULL COMMENT '维修工用户ID（关联sys_user）',
    intro        VARCHAR(200) DEFAULT NULL COMMENT '维修工简介',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在职 0停用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB COMMENT '维修工档案表';

-- -------------------------------------------------------------
-- 维修工-擅长分类关联表：一个维修工可擅长多个故障分类
-- -------------------------------------------------------------
DROP TABLE IF EXISTS repair_worker_category;
CREATE TABLE repair_worker_category (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    worker_id   BIGINT NOT NULL COMMENT '维修工档案ID（关联repair_worker）',
    category_id BIGINT NOT NULL COMMENT '故障分类ID（关联fault_category）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_worker_category (worker_id, category_id),
    INDEX idx_category_id (category_id)
) ENGINE = InnoDB COMMENT '维修工-擅长分类关联表';
