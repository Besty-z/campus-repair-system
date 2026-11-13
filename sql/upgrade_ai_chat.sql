-- =============================================================
-- AI 智能报修助手：会话表与消息表
-- 说明：每个登录用户可创建多个会话；会话下按时间顺序保存用户与 AI 的消息。
-- =============================================================

USE campus_repair;

SET NAMES utf8mb4;

-- -------------------------------------------------------------
-- AI 会话表：一个用户可拥有多个会话，按更新时间倒序展示
-- -------------------------------------------------------------
DROP TABLE IF EXISTS ai_chat_session;
CREATE TABLE ai_chat_session (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT       NOT NULL COMMENT '所属用户ID（关联sys_user）',
    title        VARCHAR(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题（取首条用户消息摘要）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_update_time (update_time)
) ENGINE = InnoDB COMMENT 'AI 智能报修助手会话表';

-- -------------------------------------------------------------
-- AI 消息表：按会话保存用户提问与 AI 回答
-- role: user=用户提问 assistant=AI回答 system=系统提示
-- -------------------------------------------------------------
DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id   BIGINT        NOT NULL COMMENT '会话ID（关联ai_chat_session）',
    role         VARCHAR(20)   NOT NULL COMMENT '角色：user用户 assistantAI system系统提示',
    content      VARCHAR(2000) NOT NULL COMMENT '消息内容',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB COMMENT 'AI 智能报修助手消息表';
