-- ============================================
-- Migration: Spell <-> Class association
-- Add class_ids column to spell table
-- ============================================

SET NAMES utf8mb4;
USE linxr;

-- 1. Add class_ids column
ALTER TABLE spell ADD COLUMN class_ids JSON AFTER duration;

-- 2. Update existing spells with class_ids
UPDATE spell SET class_ids = '["wizard","sorcerer"]' WHERE id = 'spell-magic-missile';
UPDATE spell SET class_ids = '["cleric","druid","bard","paladin","ranger"]' WHERE id = 'spell-cure-wounds';
UPDATE spell SET class_ids = '["wizard","sorcerer"]' WHERE id = 'spell-shield';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard"]' WHERE id = 'spell-sleep';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard"]' WHERE id = 'spell-mirror-image';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard","warlock"]' WHERE id = 'spell-misty-step';
UPDATE spell SET class_ids = '["wizard","bard","sorcerer","warlock"]' WHERE id = 'spell-invisibility';
UPDATE spell SET class_ids = '["bard","cleric","druid","wizard","sorcerer"]' WHERE id = 'spell-hold-person';
UPDATE spell SET class_ids = '["wizard","sorcerer"]' WHERE id = 'spell-fireball';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard","warlock","cleric"]' WHERE id = 'spell-counterspell';
UPDATE spell SET class_ids = '["wizard","sorcerer","warlock"]' WHERE id = 'spell-fly';
UPDATE spell SET class_ids = '["wizard","sorcerer"]' WHERE id = 'spell-lightning-bolt';
UPDATE spell SET class_ids = '["wizard","bard","sorcerer"]' WHERE id = 'spell-greater-invisibility';
UPDATE spell SET class_ids = '["wizard","bard","druid"]' WHERE id = 'spell-polymorph';
UPDATE spell SET class_ids = '["wizard","druid","sorcerer"]' WHERE id = 'spell-ice-storm';
UPDATE spell SET class_ids = '["wizard","druid","ranger"]' WHERE id = 'spell-stoneskin';
UPDATE spell SET class_ids = '["wizard"]' WHERE id = 'spell-wall-of-force';
UPDATE spell SET class_ids = '["cleric","bard"]' WHERE id = 'spell-raise-dead';
UPDATE spell SET class_ids = '["wizard","sorcerer","druid"]' WHERE id = 'spell-cloudkill';
UPDATE spell SET class_ids = '["cleric","wizard","druid","bard","warlock"]' WHERE id = 'spell-scrying';
UPDATE spell SET class_ids = '["wizard","sorcerer"]' WHERE id = 'spell-chain-lightning';
UPDATE spell SET class_ids = '["wizard","cleric","druid","bard"]' WHERE id = 'spell-true-seeing';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard"]' WHERE id = 'spell-disintegrate';
UPDATE spell SET class_ids = '["cleric","druid"]' WHERE id = 'spell-heal';
UPDATE spell SET class_ids = '["wizard","bard","sorcerer","warlock"]' WHERE id = 'spell-teleport';
UPDATE spell SET class_ids = '["cleric","wizard","druid","warlock"]' WHERE id = 'spell-plane-shift';
UPDATE spell SET class_ids = '["wizard","sorcerer","warlock"]' WHERE id = 'spell-delayed-blast-fireball';
UPDATE spell SET class_ids = '["cleric","bard"]' WHERE id = 'spell-resurrection';
UPDATE spell SET class_ids = '["bard","wizard"]' WHERE id = 'spell-mind-blank';
UPDATE spell SET class_ids = '["druid","wizard","cleric"]' WHERE id = 'spell-sunburst';
UPDATE spell SET class_ids = '["bard","sorcerer","warlock","wizard"]' WHERE id = 'spell-power-word-stun';
UPDATE spell SET class_ids = '["druid","cleric"]' WHERE id = 'spell-earthquake';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard"]' WHERE id = 'spell-wish';
UPDATE spell SET class_ids = '["wizard","sorcerer","druid"]' WHERE id = 'spell-meteor-swarm';
UPDATE spell SET class_ids = '["wizard","sorcerer","bard"]' WHERE id = 'spell-time-stop';
UPDATE spell SET class_ids = '["bard","sorcerer","warlock","wizard"]' WHERE id = 'spell-power-word-kill';

-- 3. Insert new class-specific spells
INSERT INTO spell (id, name, subtitle, summary, level, school, casting_time, `range`, components, duration, class_ids, tags, details) VALUES
-- 圣武士 (Paladin) smite spells
('spell-divine-smite', '至圣斩', '1环 - 神圣爆发', '命中敌人后消耗法术位，附加2d8光耀伤害。对不死生物和邪魔额外1d8。圣武士的标志性爆发能力。', 1, '塑能', '1附赠动作', '自身', 'V', '立即',
 '["paladin"]', '["光耀","爆发","圣武士"]', '["环级: 1环","施法时间: 1附赠动作(命中后)","射程: 自身","成分: V","时长: 立即","效果: 命中后附加2d8光耀伤害，升环每环+1d8"]'),
('spell-thunderous-smite', '雷鸣斩', '1环 - 雷鸣冲击', '命中后附加2d6雷鸣伤害，并使目标被推退10尺。力量检定失败则倒地。', 1, '塑能', '1附赠动作', '自身', 'V,S', '立即',
 '["paladin"]', '["雷鸣","推退","圣武士"]', '["环级: 1环","施法时间: 1附赠动作(命中后)","射程: 自身","成分: V,S","时长: 立即","效果: 附加2d6雷鸣伤害+推退10尺"]'),
('spell-searing-smite', '炽烈斩', '1环 - 灼烧惩戒', '命中后附加1d6火焰伤害，目标每回合开始时再受1d6火焰伤害，持续1分钟。', 1, '塑能', '1附赠动作', '自身', 'V', '1分钟专注',
 '["paladin"]', '["火焰","持续","圣武士"]', '["环级: 1环","施法时间: 1附赠动作(命中后)","射程: 自身","成分: V","时长: 1分钟专注","效果: 附加1d6火焰+每回合持续1d6"]'),
('spell-branding-smite', '噩兆斩', '1环 - 烙印之光', '命中后附加2d6光耀伤害，且目标无法隐形直到法术结束。克制潜行敌人。', 1, '塑能', '1附赠动作', '自身', 'V,S', '1分钟专注',
 '["paladin"]', '["光耀","防隐形","圣武士"]', '["环级: 1环","施法时间: 1附赠动作(命中后)","射程: 自身","成分: V,S","时长: 1分钟专注","效果: 附加2d6光耀+无法隐形"]'),

-- 邪术师 (Warlock) spells
('spell-eldritch-blast', '魔能爆', '戏法 - 邪术师标志戏法', '射出多束力场能量远程攻击，1级时1束，每升6级多1束。邪术师最核心的输出手段。', 0, '塑能', '1动作', '300尺', 'V,S', '立即',
 '["warlock"]', '["力场","远程","戏法"]', '["环级: 戏法(0环)","施法时间: 1动作","射程: 300尺","成分: V,S","时长: 立即","效果: 1d10力场伤害，高等级多束"]'),
('spell-hex', '诅咒', '1环 - 持续标记伤害', '标记一个目标，对其造成攻击伤害时额外1d6黯蚀伤害。目标死亡后可转移诅咒。', 1, '附魔', '1附赠动作', '90尺', 'V,S,M', '1小时专注',
 '["warlock"]', '["黯蚀","持续","附赠动作"]', '["环级: 1环","施法时间: 1附赠动作","射程: 90尺","成分: V,S,M","时长: 1小时专注","效果: 额外1d6黯蚀伤害+目标对你技能检定劣势"]'),
('spell-armor-of-agathys', '阿加帝斯之甲', '1环 - 寒冰护甲', '获得5点临时HP，被近战攻击时对攻击者造成5点寒冷伤害。升环每环+5。', 1, '塑能', '1动作', '自身', 'V,S,M', '1小时',
 '["warlock"]', '["寒冷","防御","临时HP"]', '["环级: 1环","施法时间: 1动作","射程: 自身","成分: V,S,M","时长: 1小时","效果: 5临时HP+被近攻击时反伤5寒冷"]'),

-- 吟游诗人 (Bard) spells
('spell-vicious-mockery', '恶毒嘲讽', '戏法 - 言语伤害', '用刻薄言语攻击目标心智，造成1d4心灵伤害并使其下次攻击检定劣势。', 0, '附魔', '1动作', '60尺', 'V', '立即',
 '["bard"]', '["心灵","戏法","劣势"]', '["环级: 戏法(0环)","施法时间: 1动作","射程: 60尺","成分: V","时长: 立即","效果: 1d4心灵伤害+下次攻击劣势"]'),
('spell-healing-word', '治疗术(词)', '1环 - 附赠治疗', '用一个词恢复1d4+施法调整值生命值。附赠动作施法，可与主动作攻击同时使用。', 1, '塑能', '1附赠动作', '60尺', 'V', '立即',
 '["bard","cleric","druid"]', '["治疗","附赠动作","远程"]', '["环级: 1环","施法时间: 1附赠动作","射程: 60尺","成分: V","时长: 立即","效果: 恢复1d4+施法属性调整值HP"]'),
('spell-dissonant-whispers', '不谐低语', '1环 - 心灵尖啸', '对目标造成3d6心灵伤害，并迫使其用反应尽可能远离你。', 1, '附魔', '1动作', '60尺', 'V', '立即',
 '["bard"]', '["心灵","位移","反应"]', '["环级: 1环","施法时间: 1动作","射程: 60尺","成分: V","时长: 立即","效果: 3d6心灵伤害+迫使目标用反应远离"]'),

-- 牧师 (Cleric) spells
('spell-bless', '祝福术', '1环 - 豁免与命中增益', '最多3个盟友在攻击检定和豁免检定中获得1d4加值。持续战斗中的核心增益。', 1, '附魔', '1动作', '30尺', 'V,S,M', '1分钟专注',
 '["cleric","paladin"]', '["增益","1d4","专注"]', '["环级: 1环","施法时间: 1动作","射程: 30尺","成分: V,S,M","时长: 1分钟专注","效果: 3个盟友攻击和豁免+1d4"]'),
('spell-guiding-bolt', '导引之光', '1环 - 光耀制导', '远程法术攻击4d6光耀伤害，使下次对目标的攻击检定具有优势。', 1, '塑能', '1动作', '120尺', 'V,S', '1轮',
 '["cleric"]', '["光耀","远程","优势"]', '["环级: 1环","施法时间: 1动作","射程: 120尺","成分: V,S","时长: 1轮","效果: 4d6光耀伤害+下次攻击对此目标有优势"]'),
('spell-spiritual-weapon', '灵性武器', '2环 - 浮空武器', '创造一把魔法武器浮空攻击，造成1d8+施法属性调整值力场伤害。附赠动作操控。', 2, '塑能', '1附赠动作', '60尺', 'V,S', '1分钟',
 '["cleric"]', '["力场","附赠动作","持续"]', '["环级: 2环","施法时间: 1附赠动作","射程: 60尺","成分: V,S","时长: 1分钟","效果: 创造浮空武器1d8+调整值力场伤害"]'),

-- 德鲁伊 (Druid) spells
('spell-entangle', '缠绕术', '1环 - 自然束缚', '使20尺方形区域长出 grasping 草蔓，成为困难地形并擒抱其中的生物。', 1, '咒法', '1动作', '90尺', 'V,S', '1分钟专注',
 '["druid","ranger"]', '["控制","困难地形","擒抱"]', '["环级: 1环","施法时间: 1动作","射程: 90尺","成分: V,S","时长: 1分钟专注","效果: 困难地形+力量豁免失败则擒抱"]'),
('spell-thunderwave', '雷鸣波', '1环 - 爆发性推退', '15尺立方范围内2d8雷鸣伤害，力量豁免失败则被推退10尺并倒地。', 1, '塑能', '1动作', '自身(15尺立方)', 'V,S', '立即',
 '["druid","wizard","sorcerer"]', '["雷鸣","范围","推退"]', '["环级: 1环","施法时间: 1动作","射程: 自身(15尺立方)","成分: V,S","时长: 立即","效果: 2d8雷鸣伤害+推退10尺"]'),
('spell-call-lightning', '召雷术', '3环 - 风暴打击', '召唤风暴，每回合可用动作劈出一道3d10闪电伤害。持续10分钟。', 3, '塑能', '1动作', '120尺', 'V,S', '10分钟专注',
 '["druid"]', '["闪电","持续","风暴"]', '["环级: 3环","施法时间: 1动作","射程: 120尺","成分: V,S","时长: 10分钟专注","效果: 每回合可劈3d10闪电伤害"]'),

-- 术士 (Sorcerer) spells
('spell-chromatic-orb', '色彩球', '1环 - 多元素弹', '掷出一颗可自选伤害类型(酸/寒/火/电/毒/音)的能量球，3d8伤害。', 1, '塑能', '1动作', '90尺', 'V,S,M', '立即',
 '["sorcerer","wizard"]', '["多元素","远程","可变"]', '["环级: 1环","施法时间: 1动作","射程: 90尺","成分: V,S,M","时长: 立即","效果: 3d8酸/寒/火/电/毒/音伤害"]'),
('spell-chaos-bolt', '混沌箭', '1环 - 不稳定的混沌能量', '远程法术攻击2d8随机伤害类型，若双骰同点则跳跃到另一目标。', 1, '塑能', '1动作', '120尺', 'V,S', '立即',
 '["sorcerer"]', '["随机","跳跃","混沌"]', '["环级: 1环","施法时间: 1动作","射程: 120尺","成分: V,S","时长: 立即","效果: 2d8随机类型伤害，同点则跳跃"]'),

-- 法师 (Wizard) spells
('spell-detect-magic', '侦测魔法', '1环 - 感知魔法灵光', '感知30尺内任何可见的魔法灵光，可辨别学派。仪式施法不消耗法术位。', 1, '预言', '1动作', '自身', 'V,S', '10分钟专注',
 '["wizard","bard","cleric","druid","ranger"]', '["侦测","仪式","专注"]', '["环级: 1环","施法时间: 1动作(可仪式)","射程: 自身","成分: V,S","时长: 10分钟专注","效果: 感知30尺内魔法灵光"]'),
('spell-mage-armor', '法师护甲', '1环 - 施法者防护', '自身获得13+敏捷调整值的AC，持续8小时。无甲施法者的核心防护。', 1, '防护', '1动作', '触及', 'V,S,M', '8小时',
 '["wizard","sorcerer"]', '["防御","AC","长效"]', '["环级: 1环","施法时间: 1动作","射程: 触及","成分: V,S,M","时长: 8小时","效果: AC=13+敏捷调整值"]'),
('spell-burning-hands', '燃烧之手', '1环 - 锥形火焰', '前方15尺锥形区域3d6火焰伤害，敏捷豁免减半。近距离清场法术。', 1, '塑能', '1动作', '自身(15尺锥)', 'V,S', '立即',
 '["wizard","sorcerer"]', '["火焰","锥形","范围"]', '["环级: 1环","施法时间: 1动作","射程: 自身(15尺锥)","成分: V,S","时长: 立即","效果: 3d6火焰伤害(敏捷减半)"]'),

-- 游侠 (Ranger) spells
('spell-hunters-mark', '猎人印记', '1环 - 标记追猎', '标记目标，对其造成的武器伤害额外1d6。目标死亡后可转移标记。', 1, '预言', '1附赠动作', '90尺', 'V', '1小时专注',
 '["ranger"]', '["标记","额外伤害","专注"]', '["环级: 1环","施法时间: 1附赠动作","射程: 90尺","成分: V","时长: 1小时专注","效果: 武器伤害+1d6，可转移"]'),
('spell-hail-of-thorns', '荆棘之雹', '1环 - 爆裂箭矢', '下次远程命中后，以目标为中心5尺半径爆发1d10穿刺伤害。', 1, '塑能', '1附赠动作', '自身', 'V', '立即',
 '["ranger"]', '["穿刺","范围","附赠动作"]', '["环级: 1环","施法时间: 1附赠动作","射程: 自身","成分: V","时长: 立即","效果: 命中后5尺半径1d10穿刺伤害"]');
