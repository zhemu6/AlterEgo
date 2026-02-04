-- =============================================
-- AlterEgo (异我) 数据库建表语句
-- 设计风格参考：sys_user 表
-- =============================================

-- 1. 建库
CREATE DATABASE IF NOT EXISTS alterego DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

USE alterego;

-- =============================================
-- 2.1 用户表 (sys_user)
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_account`  varchar(256) NOT NULL COMMENT '账号',
    `user_password` varchar(512) NOT NULL COMMENT '密码',
    `user_name`     varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`   varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`  varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`     varchar(256)          DEFAULT 'user' NOT NULL COMMENT '用户角色：user/admin',
    `user_status`   tinyint      NOT NULL DEFAULT '0' COMMENT '状态 0-正常 1-禁用 2-待审核',
    `email`         varchar(256)          DEFAULT NULL COMMENT '邮箱',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`user_account`),
    KEY `idx_user_name` (`user_name`),
    KEY `idx_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- =============================================
-- 2.2 物种表 (species)
-- =============================================
CREATE TABLE IF NOT EXISTS `species`
(
    `id`          int          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(50)  NOT NULL COMMENT '物种名称：猪、狗、马、猫、兔等',
    `icon`        varchar(512)          DEFAULT NULL COMMENT '物种图标URL或emoji',
    `description` varchar(512)          DEFAULT NULL COMMENT '物种描述',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_species_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='物种表';

-- =============================================
-- 2.3 Agent 表 (agent)
-- =============================================
CREATE TABLE IF NOT EXISTS `agent`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`             bigint       NOT NULL COMMENT '用户ID',
    `species_id`          int          NOT NULL COMMENT '物种ID',
    `agent_name`          varchar(100) NOT NULL COMMENT 'Agent名称',
    `avatar_url`          varchar(512)          DEFAULT NULL COMMENT 'Agent头像URL',
    `personality`         text                  DEFAULT NULL COMMENT 'Agent性格描述',
    `energy`              int          NOT NULL DEFAULT '100' COMMENT '能量值，上限100',
    `post_count`    int       NOT NULL COMMENT '累计发帖数',
    `comment_count`    int       NOT NULL COMMENT '累计评论数',
    `like_count`    int       NOT NULL DEFAULT '0' COMMENT '累计获赞数',
    `dislike_count`    int       NOT NULL DEFAULT '0' COMMENT '累计获踩数',
    `last_energy_reset`   date                  DEFAULT NULL COMMENT '上次能量重置日期（用于每日0点重置）',
    `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`           tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_species_id` (`species_id`),
    KEY `idx_agent_name` (`agent_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Agent表（每个用户一个Agent）';

-- =============================================
-- 2.4 帖子表 (post)
-- =============================================
CREATE TABLE IF NOT EXISTS `post`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `agent_id`    bigint       NOT NULL COMMENT '发帖的AgentID',
    `post_type`   varchar(20)  NOT NULL DEFAULT 'normal' COMMENT '帖子类型：normal-普通帖, pk-PK帖',
    `title`       varchar(200) NOT NULL COMMENT '帖子标题',
    `content`     text                  DEFAULT NULL COMMENT '帖子内容（普通帖有，PK帖可为空）',
    `tags`        json                  DEFAULT NULL COMMENT '标签列表（JSON字符串）',
    `like_count`  int          NOT NULL DEFAULT '0' COMMENT '点赞数',
    `dislike_count` int        NOT NULL DEFAULT '0' COMMENT '点踩数',
    `comment_count` int        NOT NULL DEFAULT '0' COMMENT '评论数（冗余字段，便于排序）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_post_type` (`post_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子表';

-- =============================================
-- 2.5 PK投票选项表 (pk_vote_option)
-- =============================================
CREATE TABLE IF NOT EXISTS `pk_vote_option`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id`     bigint       NOT NULL COMMENT '关联的PK帖子ID',
    `question`    varchar(200) NOT NULL COMMENT 'PK问题',
    `option_a`    varchar(100) NOT NULL COMMENT '选项A内容',
    `option_b`    varchar(100) NOT NULL COMMENT '选项B内容',
    `vote_a_count` int         NOT NULL DEFAULT '0' COMMENT '选项A的票数',
    `vote_b_count` int         NOT NULL DEFAULT '0' COMMENT '选项B的票数',
    `status`      varchar(20)  NOT NULL DEFAULT 'active' COMMENT 'PK状态：active-进行中, closed-已结束',
    `end_time`    datetime     NOT NULL COMMENT 'PK结束时间（创建时间+24小时）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_id` (`post_id`),
    KEY `idx_status` (`status`),
    KEY `idx_end_time` (`end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='PK投票选项表（每个PK帖对应一条记录，24小时自动结束）';

-- =============================================
-- 2.6 Agent投票记录表 (agent_vote_record)
-- =============================================
CREATE TABLE IF NOT EXISTS `agent_vote_record`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id`     bigint      NOT NULL COMMENT 'PK帖子ID',
    `agent_id`    bigint      NOT NULL COMMENT '投票的AgentID',
    `vote_option` varchar(10) NOT NULL COMMENT '投票选项：A 或 B',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_agent` (`post_id`, `agent_id`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Agent投票记录表（防止重复投票）';

-- =============================================
-- 2.7 评论表 (comment)
-- =============================================
CREATE TABLE IF NOT EXISTS `comment`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id`           bigint   NOT NULL COMMENT '帖子ID',
    `agent_id`          bigint   NOT NULL COMMENT '评论的AgentID',
    `parent_comment_id` bigint            DEFAULT NULL COMMENT '父评论ID（回复评论时使用，NULL表示直接评论帖子，即根评论）',
    `root_comment_id`   bigint            DEFAULT NULL COMMENT '根评论ID（用于快速查询评论树，NULL表示本身是根评论）',
    `content`           text     NOT NULL COMMENT '评论内容',
    `reply_count`       int      NOT NULL DEFAULT '0' COMMENT '回复数（冗余字段，用于热度排序）',
    `like_count`        int      NOT NULL DEFAULT '0' COMMENT '点赞数',
    `dislike_count`     int      NOT NULL DEFAULT '0' COMMENT '点踩数',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`         tinyint  NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_parent_comment_id` (`parent_comment_id`),
    KEY `idx_root_comment_id` (`root_comment_id`),
    KEY `idx_post_reply` (`post_id`, `reply_count`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评论表（支持多层嵌套，显示回复的回复，根评论按reply_count热度排序）';

-- =============================================
-- 2.8 帖子点赞点踩表 (post_like)
-- =============================================
CREATE TABLE IF NOT EXISTS `post_like`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id`     bigint      NOT NULL COMMENT '帖子ID',
    `agent_id`    bigint      NOT NULL COMMENT 'AgentID',
    `like_type`   tinyint     NOT NULL COMMENT '态度类型：1-赞, 2-踩',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_agent` (`post_id`, `agent_id`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子点赞点踩表（Agent对帖子的态度）';

-- =============================================
-- 2.8.5 评论点赞点踩表 (comment_like)
-- =============================================
CREATE TABLE IF NOT EXISTS `comment_like`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id`  bigint      NOT NULL COMMENT '评论ID',
    `agent_id`    bigint      NOT NULL COMMENT 'AgentID',
    `like_type`   tinyint     NOT NULL COMMENT '态度类型：1-赞, 2-踩',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_agent` (`comment_id`, `agent_id`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评论点赞点踩表';

-- =============================================
-- 2.9 Agent记忆表 (agent_message)
-- =============================================
CREATE TABLE IF NOT EXISTS `agent_message`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id`  varchar(64)  NOT NULL COMMENT '会话ID (用户ID_AgentID)',
    `agent_id`    bigint       NOT NULL COMMENT 'Agent ID',
    `role`        varchar(20)  NOT NULL COMMENT '角色：user, assistant, system',
    `content`     text         NOT NULL COMMENT '消息内容',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Agent记忆表（持久化对话历史）';

-- =============================================
-- 3. 初始化数据
-- =============================================

-- 3.1 插入物种数据
INSERT INTO `species` (`name`, `icon`, `description`) VALUES
                                                          ('猪', '🐷', '憨厚可爱的猪猪'),
                                                          ('狗', '🐶', '忠诚友好的狗狗'),
                                                          ('马', '🐴', '自由奔放的马儿'),
                                                          ('猫', '🐱', '高冷傲娇的猫咪'),
                                                          ('兔', '🐰', '温柔可爱的兔兔'),
                                                          ('熊', '🐻', '憨态可掬的熊熊'),
                                                          ('鸟', '🐦', '自由飞翔的小鸟'),
                                                          ('鱼', '🐟', '灵动游弋的鱼儿');