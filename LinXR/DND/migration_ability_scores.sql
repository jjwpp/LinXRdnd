SET NAMES utf8mb4;

-- ============================================
-- Migration: Ability scores + multi-target spell flags + inventory defaults
-- ============================================

USE linxr;

-- ── 1. Player_character 表添加六维属性列 ──
ALTER TABLE player_character ADD COLUMN strength INT DEFAULT 10 COMMENT '力量(STR)';
ALTER TABLE player_character ADD COLUMN dexterity INT DEFAULT 10 COMMENT '敏捷(DEX)';
ALTER TABLE player_character ADD COLUMN constitution INT DEFAULT 10 COMMENT '体质(CON)';
ALTER TABLE player_character ADD COLUMN intelligence INT DEFAULT 10 COMMENT '智力(INT)';
ALTER TABLE player_character ADD COLUMN wisdom INT DEFAULT 10 COMMENT '感知(WIS)';
ALTER TABLE player_character ADD COLUMN charisma INT DEFAULT 10 COMMENT '魅力(CHA)';

-- ── 2. 按职业分配标准阵列 [15, 14, 13, 12, 10, 8] ──
-- 标准阵列按职业优先级分配到最关键属性

-- 战士/Fighter: STR > CON > DEX > WIS > CHA > INT
UPDATE player_character SET strength=15, constitution=14, dexterity=13, wisdom=12, charisma=10, intelligence=8
  WHERE class_id = 'fighter' AND strength = 10;

-- 野蛮人/Barbarian: STR > CON > DEX > WIS > CHA > INT
UPDATE player_character SET strength=15, constitution=14, dexterity=13, wisdom=12, charisma=10, intelligence=8
  WHERE class_id = 'barbarian' AND strength = 10;

-- 圣武士/Paladin: STR > CHA > CON > WIS > DEX > INT
UPDATE player_character SET strength=15, charisma=14, constitution=13, wisdom=12, dexterity=10, intelligence=8
  WHERE class_id = 'paladin' AND strength = 10;

-- 游侠/Ranger: DEX > CON > WIS > STR > CHA > INT
UPDATE player_character SET dexterity=15, constitution=14, wisdom=13, strength=12, charisma=10, intelligence=8
  WHERE class_id = 'ranger' AND dexterity = 10;

-- 游荡者/Rogue: DEX > CON > INT > WIS > CHA > STR
UPDATE player_character SET dexterity=15, constitution=14, intelligence=13, wisdom=12, charisma=10, strength=8
  WHERE class_id = 'rogue' AND dexterity = 10;

-- 武僧/Monk: DEX > WIS > CON > STR > INT > CHA
UPDATE player_character SET dexterity=15, wisdom=14, constitution=13, strength=12, intelligence=10, charisma=8
  WHERE class_id = 'monk' AND dexterity = 10;

-- 法师/Wizard: INT > CON > DEX > WIS > CHA > STR
UPDATE player_character SET intelligence=15, constitution=14, dexterity=13, wisdom=12, charisma=10, strength=8
  WHERE class_id = 'wizard' AND intelligence = 10;

-- 术士/Sorcerer: CHA > CON > DEX > WIS > INT > STR
UPDATE player_character SET charisma=15, constitution=14, dexterity=13, wisdom=12, intelligence=10, strength=8
  WHERE class_id = 'sorcerer' AND charisma = 10;

-- 邪术师/Warlock: CHA > CON > DEX > WIS > INT > STR
UPDATE player_character SET charisma=15, constitution=14, dexterity=13, wisdom=12, intelligence=10, strength=8
  WHERE class_id = 'warlock' AND charisma = 10;

-- 吟游诗人/Bard: CHA > DEX > CON > WIS > INT > STR
UPDATE player_character SET charisma=15, dexterity=14, constitution=13, wisdom=12, intelligence=10, strength=8
  WHERE class_id = 'bard' AND charisma = 10;

-- 牧师/Cleric: WIS > CON > STR > DEX > INT > CHA
UPDATE player_character SET wisdom=15, constitution=14, strength=13, dexterity=12, intelligence=10, charisma=8
  WHERE class_id = 'cleric' AND wisdom = 10;

-- 德鲁伊/Druid: WIS > CON > DEX > INT > CHA > STR
UPDATE player_character SET wisdom=15, constitution=14, dexterity=13, intelligence=12, charisma=10, strength=8
  WHERE class_id = 'druid' AND wisdom = 10;

-- 兜底：未匹配职业的默认分配（均衡型）
UPDATE player_character SET strength=13, dexterity=14, constitution=15, intelligence=12, wisdom=10, charisma=8
  WHERE strength = 10 AND dexterity = 10 AND constitution = 10;

-- ── 3. 给多目标法术的 details JSON 添加 targetType: "multi" ──
-- 燃烧之手（锥形范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE id = 'spell-burning-hands' OR name LIKE '%燃烧之手%';

-- 火球术（半径范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE id = 'spell-fireball' OR name LIKE '%火球%';

-- 闪电束（直线范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE name LIKE '%闪电束%' OR name LIKE '%闪电链%';

-- 雷鸣波（锥形范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE id = 'spell-thunderwave' OR name LIKE '%雷鸣波%';

-- 黎明光辉（锥形范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE id = 'spell-radiance-of-dawn' OR name LIKE '%黎明%';

-- 召唤闪电（范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE id = 'spell-call-lightning' OR name LIKE '%召唤闪电%';

-- 冰风暴（范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE name LIKE '%冰风暴%' OR name LIKE '%冰暴%';

-- 流星爆（范围）
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.targetType', 'multi')
  WHERE name LIKE '%流星%';

-- ── 4. 给已有角色添加初始背包物品（治疗药水 x2）──
INSERT INTO character_inventory (id, character_id, item_type, item_id, item_name, quantity, equipped, obtained_at)
SELECT CONCAT(char.id, '_potion_1'), char.id, 'consumable', 'potion-of-healing', '治疗药水', 2, 0, NOW()
FROM player_character char
WHERE NOT EXISTS (
    SELECT 1 FROM character_inventory inv
    WHERE inv.character_id = char.id AND inv.item_id = 'potion-of-healing'
);

-- ── 5. 给已有角色设置默认护甲（如果尚未设置）──
UPDATE player_character SET armor_id = 'leather-armor' WHERE armor_id IS NULL OR armor_id = '';
