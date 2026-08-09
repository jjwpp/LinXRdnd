-- ============================================
-- 用户信息表
-- ============================================

USE linxr;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    id          VARCHAR(32)  PRIMARY KEY COMMENT '用户ID（雪花算法）',
    nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名（登录用）',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
