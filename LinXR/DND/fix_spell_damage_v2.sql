SET NAMES utf8mb4;
USE linxr;

-- 对于 details 是 JSON 数组的法术，用 JSON_OBJECT 包装旧数据并添加 damageDice
-- 对于 details 是 JSON 对象的法术，直接 JSON_SET

-- 戏法
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d10', 'damageType', '力场') WHERE id = 'spell-eldritch-blast' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d10', '$.damageType', '力场') WHERE id = 'spell-eldritch-blast' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d4', 'damageType', '心灵') WHERE id = 'spell-vicious-mockery' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d4', '$.damageType', '心灵') WHERE id = 'spell-vicious-mockery' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d8', 'damageType', '光耀') WHERE id = 'spell-sacred-flame' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d8', '$.damageType', '光耀') WHERE id = 'spell-sacred-flame' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-guidance' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-guidance' AND JSON_TYPE(details) = 'OBJECT';

-- 1环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '3d4+3', 'damageType', '力场') WHERE id = 'spell-magic-missile' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '3d4+3', '$.damageType', '力场') WHERE id = 'spell-magic-missile' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d8+2', 'damageType', '光耀') WHERE id = 'spell-cure-wounds' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d8+2', '$.damageType', '光耀') WHERE id = 'spell-cure-wounds' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '3d6', 'damageType', '火焰') WHERE id = 'spell-burning-hands' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '3d6', '$.damageType', '火焰') WHERE id = 'spell-burning-hands' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-mage-armor' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-mage-armor' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-bless' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-bless' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '2d8', 'damageType', '雷鸣') WHERE id = 'spell-thunderwave' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '2d8', '$.damageType', '雷鸣') WHERE id = 'spell-thunderwave' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-detect-magic' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-detect-magic' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-shield' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-shield' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-sleep' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-sleep' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '2d8', 'damageType', '光耀') WHERE id = 'spell-divine-smite' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '2d8', '$.damageType', '光耀') WHERE id = 'spell-divine-smite' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '2d6', 'damageType', '雷鸣') WHERE id = 'spell-thunderous-smite' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '2d6', '$.damageType', '雷鸣') WHERE id = 'spell-thunderous-smite' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d6', 'damageType', '光耀') WHERE id = 'spell-searing-smite' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-searing-smite' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '2d6', 'damageType', '心灵') WHERE id = 'spell-branding-smite' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '2d6', '$.damageType', '心灵') WHERE id = 'spell-branding-smite' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d6', 'damageType', '黯蚀') WHERE id = 'spell-hex' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d6', '$.damageType', '黯蚀') WHERE id = 'spell-hex' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d6+2', 'damageType', '寒冷') WHERE id = 'spell-armor-of-agathys' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d6+2', '$.damageType', '寒冷') WHERE id = 'spell-armor-of-agathys' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '3d6', 'damageType', '心灵') WHERE id = 'spell-dissonant-whispers' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '3d6', '$.damageType', '心灵') WHERE id = 'spell-dissonant-whispers' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d4+2', 'damageType', '光耀') WHERE id = 'spell-healing-word' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d4+2', '$.damageType', '光耀') WHERE id = 'spell-healing-word' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '4d6', 'damageType', '光耀') WHERE id = 'spell-guiding-bolt' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '4d6', '$.damageType', '光耀') WHERE id = 'spell-guiding-bolt' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d6', 'damageType', '光耀') WHERE id = 'spell-hunters-mark' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-hunters-mark' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d10', 'damageType', '穿刺') WHERE id = 'spell-hail-of-thorns' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '1d10', '$.damageType', '穿刺') WHERE id = 'spell-hail-of-thorns' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '3d8', 'damageType', '闪电') WHERE id = 'spell-chromatic-orb' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '3d8', '$.damageType', '闪电') WHERE id = 'spell-chromatic-orb' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '2d8', 'damageType', '力场') WHERE id = 'spell-chaos-bolt' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '2d8', '$.damageType', '力场') WHERE id = 'spell-chaos-bolt' AND JSON_TYPE(details) = 'OBJECT';

UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-entangle' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_SET(details, '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-entangle' AND JSON_TYPE(details) = 'OBJECT';

-- 2环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-hold-person' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-invisibility' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-mirror-image' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-misty-step' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '1d8+2', 'damageType', '力场') WHERE id = 'spell-spiritual-weapon' AND JSON_TYPE(details) = 'ARRAY';

-- 3环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '3d10', 'damageType', '闪电') WHERE id = 'spell-call-lightning' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-counterspell' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '8d6', 'damageType', '火焰') WHERE id = 'spell-fireball' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-fly' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '8d6', 'damageType', '闪电') WHERE id = 'spell-lightning-bolt' AND JSON_TYPE(details) = 'ARRAY';

-- 4环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-greater-invisibility' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '5d8', 'damageType', '寒冷') WHERE id = 'spell-ice-storm' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-polymorph' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-stoneskin' AND JSON_TYPE(details) = 'ARRAY';

-- 5环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '8d8', 'damageType', '毒素') WHERE id = 'spell-cloudkill' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-raise-dead' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-scrying' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-wall-of-force' AND JSON_TYPE(details) = 'ARRAY';

-- 6环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '10d8', 'damageType', '闪电') WHERE id = 'spell-chain-lightning' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '10d6+40', 'damageType', '力场') WHERE id = 'spell-disintegrate' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-heal' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-true-seeing' AND JSON_TYPE(details) = 'ARRAY';

-- 7环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '12d6', 'damageType', '火焰') WHERE id = 'spell-delayed-blast-fireball' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-plane-shift' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-resurrection' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-teleport' AND JSON_TYPE(details) = 'ARRAY';

-- 8环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-earthquake' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-mind-blank' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-power-word-stun' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '12d6', 'damageType', '光耀') WHERE id = 'spell-sunburst' AND JSON_TYPE(details) = 'ARRAY';

-- 9环
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '20d6', 'damageType', '火焰/钝击') WHERE id = 'spell-meteor-swarm' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-power-word-kill' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-time-stop' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell-wish' AND JSON_TYPE(details) = 'ARRAY';

-- 旧格式法术
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_010_absorb_elements' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_012_bane' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_014_charm_person' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_015_command' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_017_fog_cloud' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_018_find_familiar' AND JSON_TYPE(details) = 'ARRAY';
UPDATE spell SET details = JSON_OBJECT('info', details, 'damageDice', '0', 'damageType', 'None') WHERE id = 'spell_020_identify' AND JSON_TYPE(details) = 'ARRAY';
