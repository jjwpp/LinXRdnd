-- ============================================
-- Migration: player_character 新增 user_id 字段，关联用户
-- ============================================

USE linxr;

ALTER TABLE player_character ADD COLUMN user_id VARCHAR(32) DEFAULT NULL COMMENT '关联用户ID' AFTER player_name;

-- 创建索引方便按用户查询角色
CREATE INDEX idx_player_character_user_id ON player_character(user_id);
