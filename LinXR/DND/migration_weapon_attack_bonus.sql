-- ════════════════════════════════════════════════════════════
-- 武器攻击加成 迁移脚本
-- migration_weapon_attack_bonus.sql
--
-- 为所有基础武器添加 attackBonus:1
-- 装备武器后攻击检定获得 +1 加值（徒手无此加值）
-- ════════════════════════════════════════════════════════════

-- 更新已有武器的 details JSON，将 attackBonus 从 0 改为 1
UPDATE magic_item SET details = JSON_SET(details, '$.attackBonus', 1)
WHERE item_type = 'WEAPON' AND JSON_EXTRACT(details, '$.attackBonus') = 0;

-- ════════════════════════════════════════════════════════════
-- 迁移完成
-- ════════════════════════════════════════════════════════════
