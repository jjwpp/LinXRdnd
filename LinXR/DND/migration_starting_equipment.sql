-- ════════════════════════════════════════════════════════════
-- 职业初始装备 迁移脚本
-- migration_starting_equipment.sql
--
-- 为角色创建时按职业发放基础武器和防具补充缺失的武器物品
-- 现有: 短剑(item_weapon_shortsword), 长剑(item_weapon_longsword)
-- 新增: 手斧, 细剑, 钉锤, 镰刀, 匕首
-- ════════════════════════════════════════════════════════════

-- ── 1. 插入新的基础武器（如果不存在） ──

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_dagger', '匕首', 'Dagger', '轻巧的短刃，施法者和潜行者的常用武器。', 'WEAPON', 'COMMON', 0, '["武器","近战","轻型"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":1,"damageDice":"1d4","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_handaxe', '手斧', 'Handaxe', '单手使用的投掷斧，野蛮人的首选。', 'WEAPON', 'COMMON', 0, '["武器","近战","轻型"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":1,"damageDice":"1d6","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_rapier', '细剑', 'Rapier', '修长的穿刺剑，吟游诗人和游荡者的优雅之选。', 'WEAPON', 'COMMON', 0, '["武器","近战","灵巧"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":1,"damageDice":"1d8","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_mace', '钉锤', 'Mace', '钝击武器，牧师和圣武士的标准配备。', 'WEAPON', 'COMMON', 0, '["武器","近战","简易"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":1,"damageDice":"1d6","damageType":"物理"}');

INSERT IGNORE INTO magic_item (id, name, subtitle, summary, item_type, rarity, attunement, tags, details) VALUES
('item_weapon_scimitar', '镰刀', 'Scimitar', '弧形刀刃的轻型武器，德鲁伊和游侠的偏爱。', 'WEAPON', 'COMMON', 0, '["武器","近战","灵巧"]', '{"effectType":"SPECIAL","specialType":"EQUIP","slot":"WEAPON","attackBonus":1,"damageDice":"1d6","damageType":"物理"}');

-- ════════════════════════════════════════════════════════════
-- 迁移完成
-- ════════════════════════════════════════════════════════════
