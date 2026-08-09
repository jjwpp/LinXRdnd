-- ════════════════════════════════════════════════════════════
-- 背包系统 + 怪物掉落系统 迁移脚本
-- migration_inventory_system.sql
-- ════════════════════════════════════════════════════════════

-- ── 1. 放宽 character_inventory 表的 NOT NULL 约束 ──
ALTER TABLE character_inventory MODIFY COLUMN item_type VARCHAR(50) DEFAULT NULL COMMENT '类型（从magic_item同步）';
ALTER TABLE character_inventory MODIFY COLUMN item_name VARCHAR(200) DEFAULT NULL COMMENT '物品名称（从magic_item同步）';

-- ── 2. 创建 monster_drop 表（怪物掉落配置） ──
CREATE TABLE IF NOT EXISTS monster_drop (
    id VARCHAR(100) PRIMARY KEY,
    monster_id VARCHAR(100) NOT NULL COMMENT '关联 monster.id',
    item_id VARCHAR(100) NOT NULL COMMENT '关联 magic_item.id',
    drop_rate DOUBLE DEFAULT 0.3 COMMENT '掉落概率 0.0~1.0',
    min_count INT DEFAULT 1 COMMENT '最小掉落数量',
    max_count INT DEFAULT 1 COMMENT '最大掉落数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_monster (monster_id),
    INDEX idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='怪物掉落配置表';

-- ── 3. 插入测试用 magic_item 物品（如果不存在） ──
INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_healing', '生命药水', 'Healing Potion', '一瓶散发着红色微光的药水，饮用后可恢复生命值。', 'POTION', 'COMMON', 0, '["消耗品","治疗"]', '{"effectType":"HEAL","value":50,"actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_healing_greater', '大型生命药水', 'Greater Healing Potion', '浓郁的红药水，能恢复大量生命值。', 'POTION', 'UNCOMMON', 0, '["消耗品","治疗"]', '{"effectType":"HEAL","value":"4d4+4","actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_strength', '力量药水', 'Potion of Strength', '饮用后力量大增，持续3回合。', 'POTION', 'UNCOMMON', 0, '["消耗品","增益"]', '{"effectType":"BUFF","attribute":"STRENGTH","value":5,"duration":3,"actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_dexterity', '敏捷药水', 'Potion of Dexterity', '饮用后身手敏捷，持续3回合。', 'POTION', 'UNCOMMON', 0, '["消耗品","增益"]', '{"effectType":"BUFF","attribute":"DEXTERITY","value":5,"duration":3,"actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_mana', '法力药水', 'Potion of Mana', '蓝色药水，恢复1个已消耗的法术位。', 'POTION', 'UNCOMMON', 0, '["消耗品","法力"]', '{"effectType":"MANA","value":1,"actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_potion_antidote', '解毒药水', 'Antidote', '清除体内毒素和异常状态。', 'POTION', 'COMMON', 0, '["消耗品","解救"]', '{"effectType":"REMOVE_DEBUFF","actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_scroll_fireball', '火球术卷轴', 'Scroll of Fireball', '记载着火球术的魔法卷轴，使用后对敌人造成火焰伤害。', 'SCROLL', 'RARE', 0, '["消耗品","伤害"]', '{"effectType":"DAMAGE","damageDice":"8d6","damageType":"火焰","actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_shortsword', '短剑', 'Shortsword', '一把精良的短剑，适合灵巧的战士。', 'WEAPON', 'COMMON', 0, '["武器","近战"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":0,"damageDice":"1d6","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_longsword', '长剑', 'Longsword', '标准的战士武器，威力不俗。', 'WEAPON', 'COMMON', 0, '["武器","近战"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":0,"damageDice":"1d8","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_armor_leather', '皮甲', 'Leather Armor', '轻便的皮革护甲，提供基础防护。', 'ARMOR', 'COMMON', 0, '["护甲","轻型"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"ARMOR","acBonus":2}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_armor_chain', '链甲', 'Chain Mail', '由金属环编织而成的重甲。', 'ARMOR', 'UNCOMMON', 0, '["护甲","中型"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"ARMOR","acBonus":4}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_scroll_revive', '复活卷轴', 'Scroll of Revival', '神圣的卷轴，能将倒下的冒险者复活。', 'SCROLL', 'RARE', 0, '["消耗品","复活"]', '{"effectType":"SPECIAL","specialType":"REVIVE","actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_scroll_teleport', '传送卷轴', 'Scroll of Teleport', '使用后可传送到安全地带，脱离战斗。', 'SCROLL', 'RARE', 0, '["消耗品","传送"]', '{"effectType":"SPECIAL","specialType":"TELEPORT","actionCost":1}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_ring_strength', '力量之戒', 'Ring of Strength', '镶嵌着红宝石的戒指，佩戴后力量提升。', 'RING', 'RARE', 1, '["饰品","增益"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"RING","attribute":"STRENGTH","value":2}');

-- ── 4. 插入怪物掉落配置 ──
-- 先清除旧的测试数据
DELETE FROM monster_drop WHERE id LIKE 'drop_%';

-- 地精掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_goblin_1', 'goblin', 'item_potion_healing', 0.30, 1, 2),
('drop_goblin_2', 'goblin', 'item_weapon_shortsword', 0.10, 1, 1),
('drop_goblin_3', 'goblin', 'item_potion_antidote', 0.15, 1, 1);

-- 狗头人掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_kobold_1', 'kobold', 'item_potion_healing', 0.20, 1, 1),
('drop_kobold_2', 'kobold', 'item_potion_antidote', 0.10, 1, 1);

-- 骷髅掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_skeleton_1', 'skeleton', 'item_weapon_shortsword', 0.20, 1, 1),
('drop_skeleton_2', 'skeleton', 'item_armor_chain', 0.08, 1, 1),
('drop_skeleton_3', 'skeleton', 'item_potion_healing', 0.25, 1, 2);

-- 食人魔掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_ogre_1', 'ogre', 'item_weapon_longsword', 0.25, 1, 1),
('drop_ogre_2', 'ogre', 'item_armor_leather', 0.15, 1, 1),
('drop_ogre_3', 'ogre', 'item_potion_healing', 0.40, 1, 2),
('drop_ogre_4', 'ogre', 'item_potion_strength', 0.15, 1, 1);

-- 巨魔掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_troll_1', 'troll', 'item_potion_healing_greater', 0.30, 1, 2),
('drop_troll_2', 'troll', 'item_weapon_longsword', 0.20, 1, 1),
('drop_troll_3', 'troll', 'item_armor_chain', 0.15, 1, 1);

-- 枭熊掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_owlbear_1', 'owlbear', 'item_potion_healing', 0.35, 1, 2),
('drop_owlbear_2', 'owlbear', 'item_potion_healing_greater', 0.15, 1, 1);

-- 红龙掉落（稀有）
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_dragon_1', 'red-dragon', 'item_ring_strength', 0.50, 1, 1),
('drop_dragon_2', 'red-dragon', 'item_potion_healing_greater', 0.80, 2, 3),
('drop_dragon_3', 'red-dragon', 'item_scroll_fireball', 0.40, 1, 2),
('drop_dragon_4', 'red-dragon', 'item_scroll_revive', 0.20, 1, 1);

-- 巫妖掉落（传奇）
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_lich_1', 'lich', 'item_scroll_revive', 0.40, 1, 1),
('drop_lich_2', 'lich', 'item_scroll_teleport', 0.35, 1, 1),
('drop_lich_3', 'lich', 'item_potion_healing_greater', 0.50, 1, 2);

-- 拟形怪掉落
INSERT INTO monster_drop (id, monster_id, item_id, drop_rate, min_count, max_count) VALUES
('drop_mimic_1', 'mimic', 'item_potion_healing', 0.30, 1, 2),
('drop_mimic_2', 'mimic', 'item_weapon_longsword', 0.15, 1, 1),
('drop_mimic_3', 'mimic', 'item_scroll_fireball', 0.10, 1, 1);

-- ════════════════════════════════════════════════════════════
-- 迁移完成
-- ════════════════════════════════════════════════════════════
