-- ================================================================
-- 图片字段迁移：为 monster 和 player_character 表添加图片 URL 列
-- 图片存储在 MinIO 中，数据库仅存 URL 路径
-- ================================================================

-- monster 表：添加 image_url 列（怪物立绘图片）
ALTER TABLE monster ADD COLUMN image_url VARCHAR(500) DEFAULT NULL COMMENT 'MinIO图片URL' AFTER damage_type;

-- player_character 表：添加男女角色图片列
ALTER TABLE player_character ADD COLUMN male_image_url VARCHAR(500) DEFAULT NULL COMMENT '男性角色立绘URL' AFTER gender;
ALTER TABLE player_character ADD COLUMN female_image_url VARCHAR(500) DEFAULT NULL COMMENT '女性角色立绘URL' AFTER male_image_url;
