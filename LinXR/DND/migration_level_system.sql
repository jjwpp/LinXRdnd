-- ════════════════════════════════════════════════════════════
-- 角色升级系统 迁移脚本
-- 创建 class_level_progression 表 + 12个职业 × 2-12级 成长数据
-- ════════════════════════════════════════════════════════════

SET NAMES utf8mb4;

-- ── 1. 创建职业成长表 ──
CREATE TABLE IF NOT EXISTS class_level_progression (
    id VARCHAR(100) PRIMARY KEY,
    class_id VARCHAR(50) NOT NULL COMMENT '职业ID (fighter/barbarian/...)',
    level INT NOT NULL COMMENT '目标等级 (2-12)',
    reward_type VARCHAR(50) NOT NULL COMMENT 'ABILITY/ASI/NEW_SPELL/COMBAT_STYLE/FEAT_CHOICE',
    reward_name VARCHAR(200) NOT NULL COMMENT '奖励名称',
    description TEXT COMMENT '描述',
    reward_data JSON COMMENT '详细数据',
    sort_order INT DEFAULT 0 COMMENT '同等级内排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_class_level (class_id, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业等级成长配置';

-- ════════════════════════════════════════════════════════════
-- 2. 插入12个职业的升级数据 (2-12级)
-- reward_type 说明:
--   ABILITY        - 职业能力 (自动获得)
--   ASI            - 属性提升 (玩家选择: 属性+2 或 专长)
--   NEW_SPELL      - 学习新法术 (玩家选择)
--   COMBAT_STYLE   - 战斗风格选择 (玩家选择)
--   FEAT_CHOICE    - 专长选择 (玩家选择)
-- ════════════════════════════════════════════════════════════

-- ── 战士 Fighter ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('fighter_2', 'fighter', 2, 'ABILITY', '动作如潮', '额外获得一次动作，可在战斗中使用。', '{"effect":"action_surge","passive":true}', 0),
('fighter_3', 'fighter', 3, 'COMBAT_STYLE', '战斗风格', '选择一种战斗风格来强化你的战斗方式。', '{"options":[{"id":"defense","name":"防御","effect":"ac+1","description":"AC+1"},{"id":"two_weapon","name":"双武器","effect":"offhand_boost","description":"副手攻击强化"},{"id":"archery","name":"弓术","effect":"ranged_hit+2","description":"远程攻击命中+2"},{"id":"great_weapon","name":"重武器","effect":"damage_boost","description":"重武器伤害增加"}]}', 0),
('fighter_4', 'fighter', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('fighter_5', 'fighter', 5, 'ABILITY', '额外攻击', '攻击动作可以攻击两次。', '{"effect":"extra_attack","attacks":2,"passive":true}', 0),
('fighter_6', 'fighter', 6, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('fighter_7', 'fighter', 7, 'ABILITY', '战斗技巧强化', '战斗经验使你的技巧更加纯熟。', '{"effect":"combat_mastery","passive":true}', 0),
('fighter_8', 'fighter', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('fighter_9', 'fighter', 9, 'ABILITY', '不屈意志', '豁免失败时可以重新判定。', '{"effect":"indomitable","passive":true}', 0),
('fighter_10', 'fighter', 10, 'ABILITY', '战斗能力强化', '你的战斗能力得到全面提升。', '{"effect":"combat_enhancement","passive":true}', 0),
('fighter_11', 'fighter', 11, 'ABILITY', '额外攻击强化', '攻击动作可以攻击三次。', '{"effect":"extra_attack","attacks":3,"passive":true}', 0),
('fighter_12', 'fighter', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 野蛮人 Barbarian ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('barbarian_2_1', 'barbarian', 2, 'ABILITY', '危险感知', '你拥有超乎常人的危险感知能力。', '{"effect":"danger_sense","passive":true}', 0),
('barbarian_2_2', 'barbarian', 2, 'ABILITY', '狂暴强化', '狂暴时获得更多优势。', '{"effect":"rage_boost","passive":true}', 1),
('barbarian_3', 'barbarian', 3, 'ABILITY', '狂暴能力强化', '狂暴的伤害和效果得到提升。', '{"effect":"primal_path","passive":true}', 0),
('barbarian_4', 'barbarian', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('barbarian_5_1', 'barbarian', 5, 'ABILITY', '额外攻击', '攻击动作可以攻击两次。', '{"effect":"extra_attack","attacks":2,"passive":true}', 0),
('barbarian_5_2', 'barbarian', 5, 'ABILITY', '移动速度增加', '你的移动速度增加10尺。', '{"effect":"fast_movement","passive":true}', 1),
('barbarian_6', 'barbarian', 6, 'ABILITY', '野蛮人抗性', '狂暴时对钝击、穿刺和挥砍伤害获得抗性。', '{"effect":"damage_resistance","passive":true}', 0),
('barbarian_7', 'barbarian', 7, 'ABILITY', '野性直觉', '先攻检定获得优势。', '{"effect":"feral_instinct","passive":true}', 0),
('barbarian_8', 'barbarian', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('barbarian_9', 'barbarian', 9, 'ABILITY', '狂暴伤害提升', '狂暴时的近战伤害加值提升。', '{"effect":"brutal_critical","passive":true}', 0),
('barbarian_10', 'barbarian', 10, 'ABILITY', '恐怖存在', '你的存在使敌人战斗能力下降。', '{"effect":"intimidating_presence","passive":true}', 0),
('barbarian_11', 'barbarian', 11, 'ABILITY', '不屈狂怒', '濒死时可以继续战斗。', '{"effect":"relentless_rage","passive":true}', 0),
('barbarian_12', 'barbarian', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 游侠 Ranger ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('ranger_2_1', 'ranger', 2, 'COMBAT_STYLE', '战斗风格', '选择一种战斗风格来强化你的战斗方式。', '{"options":[{"id":"two_weapon","name":"双武器","effect":"offhand_boost","description":"副手攻击强化"},{"id":"archery","name":"弓箭","effect":"ranged_hit+2","description":"远程攻击命中+2"},{"id":"defense","name":"防御","effect":"ac+1","description":"AC+1"}]}', 0),
('ranger_2_2', 'ranger', 2, 'NEW_SPELL', '学习1环法术', '学会两个新的1环游侠法术。', '{"maxSpellLevel":1}', 1),
('ranger_3', 'ranger', 3, 'ABILITY', '游侠专长', '获得游侠专属能力。', '{"effect":"ranger_archetype","passive":true}', 0),
('ranger_4', 'ranger', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('ranger_5', 'ranger', 5, 'ABILITY', '额外攻击', '攻击动作可以攻击两次。', '{"effect":"extra_attack","attacks":2,"passive":true}', 0),
('ranger_6', 'ranger', 6, 'ABILITY', '强化探索', '你的野外探索能力得到提升。', '{"effect":"favored_enemy_boost","passive":true}', 0),
('ranger_7', 'ranger', 7, 'ABILITY', '野外适应', '在各种地形中获得适应能力。', '{"effect":"natural_explorer_boost","passive":true}', 0),
('ranger_8', 'ranger', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('ranger_9', 'ranger', 9, 'ABILITY', '法术强化', '3环以下法术效果增强。', '{"effect":"spell_enhancement","passive":true}', 0),
('ranger_10', 'ranger', 10, 'ABILITY', '隐藏大师', '你在自然环境中可以完美隐藏。', '{"effect":"hide_in_plain_sight","passive":true}', 0),
('ranger_11', 'ranger', 11, 'ABILITY', '攻击强化', '你的攻击能力得到全面提升。', '{"effect":"attack_enhancement","passive":true}', 0),
('ranger_12', 'ranger', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 游荡者 Rogue ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('rogue_2', 'rogue', 2, 'ABILITY', '灵巧动作', '你的反应速度让你可以更好地利用附赠动作。', '{"effect":"cunning_action","passive":true}', 0),
('rogue_3', 'rogue', 3, 'ABILITY', '偷袭伤害提升', '偷袭骰增加到2d6。', '{"effect":"sneak_attack","dice":2,"passive":true}', 0),
('rogue_4', 'rogue', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('rogue_5', 'rogue', 5, 'ABILITY', '反应闪避', '面对危险时可以使用反应来减少伤害。', '{"effect":"uncanny_dodge","passive":true}', 0),
('rogue_6', 'rogue', 6, 'ABILITY', '技能专精提升', '选择两项技能获得专精加成。', '{"effect":"expertise","passive":true}', 0),
('rogue_7', 'rogue', 7, 'ABILITY', '危险感知', '你对陷阱和危险的感知能力提升。', '{"effect":"evasion","passive":true}', 0),
('rogue_8', 'rogue', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('rogue_9', 'rogue', 9, 'ABILITY', '偷袭强化', '偷袭骰增加到5d6。', '{"effect":"sneak_attack","dice":5,"passive":true}', 0),
('rogue_10', 'rogue', 10, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('rogue_11', 'rogue', 11, 'ABILITY', '可靠能力', '你在压力下也能稳定发挥。', '{"effect":"reliable_talent","passive":true}', 0),
('rogue_12', 'rogue', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 法师 Wizard ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('wizard_2', 'wizard', 2, 'ABILITY', '奥术恢复', '短休时可以恢复部分法术位。', '{"effect":"arcane_recovery","passive":true}', 0),
('wizard_3', 'wizard', 3, 'NEW_SPELL', '学习2环法术', '学会两个新的2环法术。', '{"maxSpellLevel":2}', 0),
('wizard_4', 'wizard', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('wizard_5', 'wizard', 5, 'NEW_SPELL', '学习3环法术', '学会两个新的3环法术。', '{"maxSpellLevel":3}', 0),
('wizard_6', 'wizard', 6, 'ABILITY', '法术强化', '你的法术效果得到增强。', '{"effect":"spell_mastery","passive":true}', 0),
('wizard_7', 'wizard', 7, 'NEW_SPELL', '学习4环法术', '学会一个新的4环法术。', '{"maxSpellLevel":4}', 0),
('wizard_8', 'wizard', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('wizard_9', 'wizard', 9, 'NEW_SPELL', '学习5环法术', '学会一个新的5环法术。', '{"maxSpellLevel":5}', 0),
('wizard_10', 'wizard', 10, 'ABILITY', '法术强化', '你的法术能力全面提升。', '{"effect":"spell_enhancement","passive":true}', 0),
('wizard_11', 'wizard', 11, 'NEW_SPELL', '学习6环法术', '学会一个新的6环法术。', '{"maxSpellLevel":6}', 0),
('wizard_12', 'wizard', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 术士 Sorcerer ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('sorcerer_2', 'sorcerer', 2, 'ABILITY', '魔力点', '获得魔力点系统，可以灵活转换法术位。', '{"effect":"font_of_magic","passive":true}', 0),
('sorcerer_3', 'sorcerer', 3, 'ABILITY', '超魔选择', '选择一种超魔能力来改变你的法术。', '{"effect":"metamagic","passive":true}', 0),
('sorcerer_4', 'sorcerer', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('sorcerer_5', 'sorcerer', 5, 'NEW_SPELL', '学习3环法术', '学会一个新的3环法术。', '{"maxSpellLevel":3}', 0),
('sorcerer_6', 'sorcerer', 6, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('sorcerer_7', 'sorcerer', 7, 'NEW_SPELL', '学习4环法术', '学会一个新的4环法术。', '{"maxSpellLevel":4}', 0),
('sorcerer_8', 'sorcerer', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('sorcerer_9', 'sorcerer', 9, 'NEW_SPELL', '学习5环法术', '学会一个新的5环法术。', '{"maxSpellLevel":5}', 0),
('sorcerer_10', 'sorcerer', 10, 'ABILITY', '超魔强化', '获得新的超魔能力选择。', '{"effect":"metamagic_boost","passive":true}', 0),
('sorcerer_11', 'sorcerer', 11, 'NEW_SPELL', '学习6环法术', '学会一个新的6环法术。', '{"maxSpellLevel":6}', 0),
('sorcerer_12', 'sorcerer', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 邪术师 Warlock ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('warlock_2', 'warlock', 2, 'ABILITY', '魔能祈唤', '获得魔能祈唤能力，从守护之力中选择。', '{"effect":"eldritch_invocations","passive":true}', 0),
('warlock_3', 'warlock', 3, 'ABILITY', '魔契能力', '选择一种魔契能力：链契、刃契或书契。', '{"effect":"pact_boon","passive":true}', 0),
('warlock_4', 'warlock', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('warlock_5', 'warlock', 5, 'ABILITY', '强化魔能', '魔能爆等核心能力得到强化。', '{"effect":"eldritch_boost","passive":true}', 0),
('warlock_6', 'warlock', 6, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('warlock_7', 'warlock', 7, 'ABILITY', '魔契强化', '魔契能力得到强化。', '{"effect":"pact_boost","passive":true}', 0),
('warlock_8', 'warlock', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('warlock_9', 'warlock', 9, 'ABILITY', '高级魔能', '获得更强大的魔能祈唤。', '{"effect":"mystic_arcanum","passive":true}', 0),
('warlock_10', 'warlock', 10, 'ABILITY', '魔能强化', '魔能能力全面强化。', '{"effect":"eldritch_master","passive":true}', 0),
('warlock_11', 'warlock', 11, 'ABILITY', '魔契强化', '魔契能力进一步提升。', '{"effect":"pact_enhancement","passive":true}', 0),
('warlock_12', 'warlock', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 圣武士 Paladin ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('paladin_2', 'paladin', 2, 'ABILITY', '圣疗强化', '圣疗池得到扩充，治疗效果提升。', '{"effect":"lay_on_hands_boost","passive":true}', 0),
('paladin_3', 'paladin', 3, 'ABILITY', '神圣誓言', '立下神圣誓言，获得誓言能力。', '{"effect":"sacred_oath","passive":true}', 0),
('paladin_4', 'paladin', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('paladin_5', 'paladin', 5, 'ABILITY', '额外攻击', '攻击动作可以攻击两次。', '{"effect":"extra_attack","attacks":2,"passive":true}', 0),
('paladin_6', 'paladin', 6, 'ABILITY', '保护光环', '10尺内友军豁免获得魅力加值。', '{"effect":"aura_of_protection","passive":true}', 0),
('paladin_7', 'paladin', 7, 'ABILITY', '誓言能力强化', '神圣誓言能力得到强化。', '{"effect":"sacred_oath_boost","passive":true}', 0),
('paladin_8', 'paladin', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('paladin_9', 'paladin', 9, 'ABILITY', '高级神术', '可以施展更高级的神圣法术。', '{"effect":"divine_smite_boost","passive":true}', 0),
('paladin_10', 'paladin', 10, 'ABILITY', '圣疗强化', '圣疗能力得到进一步提升。', '{"effect":"aura_improvement","passive":true}', 0),
('paladin_11', 'paladin', 11, 'ABILITY', '神圣打击强化', '你的武器攻击附带神圣伤害。', '{"effect":"improved_divine_smite","passive":true}', 0),
('paladin_12', 'paladin', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 牧师 Cleric ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('cleric_2', 'cleric', 2, 'ABILITY', '神圣能力强化', '引导神力使用次数增加。', '{"effect":"channel_divinity_boost","passive":true}', 0),
('cleric_3', 'cleric', 3, 'NEW_SPELL', '学习2环神术', '学会两个新的2环神术。', '{"maxSpellLevel":2}', 0),
('cleric_4', 'cleric', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('cleric_5', 'cleric', 5, 'NEW_SPELL', '学习3环神术', '学会两个新的3环神术。', '{"maxSpellLevel":3}', 0),
('cleric_6', 'cleric', 6, 'ABILITY', '神圣能力提升', '引导神力效果增强。', '{"effect":"channel_divinity_enhance","passive":true}', 0),
('cleric_7', 'cleric', 7, 'NEW_SPELL', '学习4环神术', '学会一个新的4环神术。', '{"maxSpellLevel":4}', 0),
('cleric_8', 'cleric', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('cleric_9', 'cleric', 9, 'NEW_SPELL', '学习5环神术', '学会一个新的5环神术。', '{"maxSpellLevel":5}', 0),
('cleric_10', 'cleric', 10, 'ABILITY', '神圣能力强化', '神圣干预能力提升。', '{"effect":"divine_intervention","passive":true}', 0),
('cleric_11', 'cleric', 11, 'NEW_SPELL', '学习6环神术', '学会一个新的6环神术。', '{"maxSpellLevel":6}', 0),
('cleric_12', 'cleric', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 德鲁伊 Druid ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('druid_2', 'druid', 2, 'ABILITY', '野性变形', '可以变形为CR 1/4的野兽。', '{"effect":"wild_shape","cr":"1/4","passive":true}', 0),
('druid_3', 'druid', 3, 'NEW_SPELL', '学习2环法术', '学会两个新的2环德鲁伊法术。', '{"maxSpellLevel":2}', 0),
('druid_4', 'druid', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('druid_5', 'druid', 5, 'NEW_SPELL', '学习3环法术', '学会两个新的3环法术。', '{"maxSpellLevel":3}', 0),
('druid_6', 'druid', 6, 'ABILITY', '野性变形强化', '变形CR提升至1/2。', '{"effect":"wild_shape_boost","cr":"1/2","passive":true}', 0),
('druid_7', 'druid', 7, 'NEW_SPELL', '学习4环法术', '学会一个新的4环法术。', '{"maxSpellLevel":4}', 0),
('druid_8', 'druid', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('druid_9', 'druid', 9, 'NEW_SPELL', '学习5环法术', '学会一个新的5环法术。', '{"maxSpellLevel":5}', 0),
('druid_10', 'druid', 10, 'ABILITY', '自然守护', '获得自然守护能力。', '{"effect":"natures_ward","passive":true}', 0),
('druid_11', 'druid', 11, 'NEW_SPELL', '学习6环法术', '学会一个新的6环法术。', '{"maxSpellLevel":6}', 0),
('druid_12', 'druid', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 武僧 Monk ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('monk_2_1', 'monk', 2, 'ABILITY', '气点系统', '获得气点，可以驱动武僧特殊能力。', '{"effect":"ki","passive":true}', 0),
('monk_2_2', 'monk', 2, 'ABILITY', '疾风连击', '消耗1气点进行徒手打击作为附赠动作。', '{"effect":"flurry_of_blows","passive":true}', 1),
('monk_3', 'monk', 3, 'ABILITY', '武僧技巧强化', '获得新的武僧技巧。', '{"effect":"monastic_tradition","passive":true}', 0),
('monk_4', 'monk', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('monk_5', 'monk', 5, 'ABILITY', '额外攻击', '攻击动作可以攻击两次。', '{"effect":"extra_attack","attacks":2,"passive":true}', 0),
('monk_6', 'monk', 6, 'ABILITY', '强化拳', '徒手打击获得魔法特性，可克制抗性。', '{"effect":"ki_empowered_strikes","passive":true}', 0),
('monk_7', 'monk', 7, 'ABILITY', '心如止水', '受到魅惑和恐惧时可以用动作结束状态。', '{"effect":"stillness_of_mind","passive":true}', 0),
('monk_8', 'monk', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('monk_9', 'monk', 9, 'ABILITY', '高速移动', '移动速度随等级提升而增加。', '{"effect":"unarmored_movement_boost","passive":true}', 0),
('monk_10', 'monk', 10, 'ABILITY', '身体净化', '免疫毒素和疾病。', '{"effect":"purity_of_body","passive":true}', 0),
('monk_11', 'monk', 11, 'ABILITY', '身体强化', '武僧身体能力全面提升。', '{"effect":"diamond_soul","passive":true}', 0),
('monk_12', 'monk', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);

-- ── 吟游诗人 Bard ──
INSERT IGNORE INTO class_level_progression (id, class_id, level, reward_type, reward_name, description, reward_data, sort_order) VALUES
('bard_2', 'bard', 2, 'ABILITY', '万事通', '所有未熟练的技能检定加一半熟练加值。', '{"effect":"jack_of_all_trades","passive":true}', 0),
('bard_3', 'bard', 3, 'ABILITY', '灵感强化', '吟游激励骰升级。', '{"effect":"bardic_inspiration_boost","passive":true}', 0),
('bard_4', 'bard', 4, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('bard_5', 'bard', 5, 'NEW_SPELL', '学习3环法术', '学会一个新的3环法术。', '{"maxSpellLevel":3}', 0),
('bard_6', 'bard', 6, 'ABILITY', '灵感恢复', '短休后恢复吟游激励使用次数。', '{"effect":"inspiration_recovery","passive":true}', 0),
('bard_7', 'bard', 7, 'NEW_SPELL', '学习4环法术', '学会一个新的4环法术。', '{"maxSpellLevel":4}', 0),
('bard_8', 'bard', 8, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0),
('bard_9', 'bard', 9, 'NEW_SPELL', '学习5环法术', '学会一个新的5环法术。', '{"maxSpellLevel":5}', 0),
('bard_10', 'bard', 10, 'ABILITY', '魔法秘密', '可以从任何职业的法术列表中学习法术。', '{"effect":"magical_secrets","passive":true}', 0),
('bard_11', 'bard', 11, 'NEW_SPELL', '学习6环法术', '学会一个新的6环法术。', '{"maxSpellLevel":6}', 0),
('bard_12', 'bard', 12, 'ASI', '属性提升', '选择一项属性+2，或两项属性各+1，或选择一个专长。', '{"choices":["str+2","dex+2","con+2","int+2","wis+2","cha+2","feat"]}', 0);
