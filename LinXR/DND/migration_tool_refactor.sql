SET NAMES utf8mb4;

-- ============================================
-- Migration: Tool refactor — add combat stats to monster, equipment to player_character, create inventory table
-- ============================================

USE linxr;

-- ── 1. Monster 表添加战斗属性 ──
ALTER TABLE monster ADD COLUMN attack_bonus INT DEFAULT NULL COMMENT '攻击加值';
ALTER TABLE monster ADD COLUMN damage_formula VARCHAR(30) DEFAULT NULL COMMENT '伤害骰子公式，如 1d6+2';
ALTER TABLE monster ADD COLUMN damage_type VARCHAR(20) DEFAULT NULL COMMENT '伤害类型';

-- 更新所有怪物的战斗属性（基于DND 5e官方数据）
UPDATE monster SET attack_bonus = 4,  damage_formula = '1d6+2',  damage_type = '挥砍' WHERE id = 'goblin';
UPDATE monster SET attack_bonus = 4,  damage_formula = '1d4+2',  damage_type = '穿刺' WHERE id = 'kobold';
UPDATE monster SET attack_bonus = 4,  damage_formula = '1d6+2',  damage_type = '穿刺' WHERE id = 'skeleton';
UPDATE monster SET attack_bonus = 3,  damage_formula = '1d6+1',  damage_type = '钝击' WHERE id = 'zombie';
UPDATE monster SET attack_bonus = 6,  damage_formula = '2d8+4',  damage_type = '钝击' WHERE id = 'ogre';
UPDATE monster SET attack_bonus = 7,  damage_formula = '2d6+4',  damage_type = '挥砍' WHERE id = 'troll';
UPDATE monster SET attack_bonus = 6,  damage_formula = '2d6+3',  damage_type = '挥砍' WHERE id = 'owlbear';
UPDATE monster SET attack_bonus = 5,  damage_formula = '1d8+3',  damage_type = '穿刺' WHERE id = 'mimic';
UPDATE monster SET attack_bonus = 6,  damage_formula = '1d10+3', damage_type = '挥砍' WHERE id = 'manticore';
UPDATE monster SET attack_bonus = 6,  damage_formula = '1d10+3', damage_type = '穿刺' WHERE id = 'displacer-beast';
UPDATE monster SET attack_bonus = 6,  damage_formula = '1d10+3', damage_type = '穿刺' WHERE id = 'basilisk';
UPDATE monster SET attack_bonus = 6,  damage_formula = '2d6+3',  damage_type = '挥砍' WHERE id = 'medusa';
UPDATE monster SET attack_bonus = 7,  damage_formula = '2d10+4', damage_type = '挥砍' WHERE id = 'chimera';
UPDATE monster SET attack_bonus = 11, damage_formula = '2d8+9',  damage_type = '挥砍' WHERE id = 'mind-flayer';
UPDATE monster SET attack_bonus = 13, damage_formula = '3d10+8', damage_type = '火焰' WHERE id = 'fire-giant';
UPDATE monster SET attack_bonus = 13, damage_formula = '3d12+9', damage_type = '挥砍' WHERE id = 'frost-giant';
UPDATE monster SET attack_bonus = 10, damage_formula = '2d8+6',  damage_type = '钝击' WHERE id = 'stone-golem';
UPDATE monster SET attack_bonus = 17, damage_formula = '3d10+10', damage_type = '挥砍' WHERE id = 'red-dragon';
UPDATE monster SET attack_bonus = 17, damage_formula = '3d8+8',  damage_type = '力场' WHERE id = 'beholder';
UPDATE monster SET attack_bonus = 12, damage_formula = '1d8+7',  damage_type = '黯蚀' WHERE id = 'lich';
UPDATE monster SET attack_bonus = 5,  damage_formula = '2d6+2',  damage_type = '挥砍' WHERE id = 'werewolf';
UPDATE monster SET attack_bonus = 19, damage_formula = '4d12+15', damage_type = '穿刺' WHERE id = 'tarrasque';

-- ── 2. Player_character 表添加装备字段 ──
ALTER TABLE player_character ADD COLUMN weapon_id VARCHAR(100) DEFAULT NULL COMMENT '装备的武器ID';
ALTER TABLE player_character ADD COLUMN armor_id VARCHAR(100) DEFAULT NULL COMMENT '装备的护甲ID';

-- 给已有角色设置默认装备
UPDATE player_character SET weapon_id = 'longsword' WHERE weapon_id IS NULL;

-- ── 3. 创建角色背包表 ──
CREATE TABLE IF NOT EXISTS character_inventory (
    id VARCHAR(100) PRIMARY KEY,
    character_id VARCHAR(100) NOT NULL COMMENT '角色ID',
    item_type VARCHAR(20) NOT NULL COMMENT '类型: weapon/armor/magic_item/consumable',
    item_id VARCHAR(100) NOT NULL COMMENT '物品ID',
    item_name VARCHAR(100) NOT NULL COMMENT '物品名称',
    quantity INT DEFAULT 1 COMMENT '数量',
    equipped TINYINT(1) DEFAULT 0 COMMENT '是否装备中',
    obtained_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    INDEX idx_character (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. 更新法术 details JSON，添加 damageDice 字段 ──
-- 原有法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '火焰') WHERE id = 'spell_001_fire_bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8', '$.damageType', '寒冷') WHERE id = 'spell_002_ray_of_frost';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_003_mage_hand';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_004_light';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_005_mending';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d4+3', '$.damageType', '力场') WHERE id = 'spell-magic-missile';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8+2', '$.damageType', '光耀') WHERE id = 'spell-cure-wounds';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d6', '$.damageType', '火焰') WHERE id = 'spell-burning-hands';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-mage-armor';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-bless';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '雷鸣') WHERE id = 'spell-thunderwave';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_014_charm_person';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_015_command';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_017_fog_cloud';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_018_find_familiar';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_020_identify';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_010_absorb_elements';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_012_bane';
-- 新增法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '力场') WHERE id = 'spell-eldritch-blast';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d4', '$.damageType', '心灵') WHERE id = 'spell-vicious-mockery';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-guidance';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8', '$.damageType', '光耀') WHERE id = 'spell-sacred-flame';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '光耀') WHERE id = 'spell-divine-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d6', '$.damageType', '雷鸣') WHERE id = 'spell-thunderous-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-searing-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d6', '$.damageType', '心灵') WHERE id = 'spell-wrathful-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '黯蚀') WHERE id = 'spell-hex';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6+2', '$.damageType', '寒冷') WHERE id = 'spell-armor-of-agathys';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d6', '$.damageType', '心灵') WHERE id = 'spell-dissonant-whispers';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d4+2', '$.damageType', '光耀') WHERE id = 'spell-healing-word';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-hunters-mark';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '穿刺') WHERE id = 'spell-hail-of-thorns';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '4d6', '$.damageType', '光耀') WHERE id = 'spell-guiding-bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-bless';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8+2', '$.damageType', '力场') WHERE id = 'spell-spiritual-weapon';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d8', '$.damageType', '光耀') WHERE id = 'spell-radiance-of-dawn';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d10', '$.damageType', '闪电') WHERE id = 'spell-call-lightning';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d8', '$.damageType', '闪电') WHERE id = 'spell-chromatic-orb';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '力场') WHERE id = 'spell-chaos-bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-entangle';
-- 更高环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '火焰') WHERE name LIKE '%火球%';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '火焰') WHERE name LIKE '%火球术%';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '火焰') WHERE id = 'spell-fireball';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '火焰') WHERE name LIKE '%流星%';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE name LIKE '%侦测%';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE name LIKE '%祈愿%';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE name LIKE '%疗伤%' AND `level` = 0;
