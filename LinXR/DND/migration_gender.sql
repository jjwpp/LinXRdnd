-- 为 player_character 表添加 gender 列
-- 用于存储角色性别 (male / female)，影响职业立绘图片显示
ALTER TABLE player_character ADD COLUMN gender VARCHAR(10) DEFAULT 'male' AFTER class_id;
