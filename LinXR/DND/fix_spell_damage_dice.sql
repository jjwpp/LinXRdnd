SET NAMES utf8mb4;
USE linxr;

-- 更新所有法术的 damageDice 和 damageType（基于DND 5e官方数据）
-- 戏法 (level 0)
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '力场') WHERE id = 'spell-eldritch-blast';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d4', '$.damageType', '心灵') WHERE id = 'spell-vicious-mockery';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8', '$.damageType', '光耀') WHERE id = 'spell-sacred-flame';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-guidance';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '火焰') WHERE id = 'spell_001_fire_bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8', '$.damageType', '寒冷') WHERE id = 'spell_002_ray_of_frost';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_003_mage_hand';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_004_light';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_005_mending';

-- 1环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d4+3', '$.damageType', '力场') WHERE id = 'spell-magic-missile';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8+2', '$.damageType', '光耀') WHERE id = 'spell-cure-wounds';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d6', '$.damageType', '火焰') WHERE id = 'spell-burning-hands';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-mage-armor';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-bless';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '雷鸣') WHERE id = 'spell-thunderwave';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-detect-magic';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-shield';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-sleep';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '光耀') WHERE id = 'spell-divine-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d6', '$.damageType', '雷鸣') WHERE id = 'spell-thunderous-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-searing-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d6', '$.damageType', '心灵') WHERE id = 'spell-branding-smite';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '黯蚀') WHERE id = 'spell-hex';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6+2', '$.damageType', '寒冷') WHERE id = 'spell-armor-of-agathys';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d6', '$.damageType', '心灵') WHERE id = 'spell-dissonant-whispers';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d4+2', '$.damageType', '光耀') WHERE id = 'spell-healing-word';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '4d6', '$.damageType', '光耀') WHERE id = 'spell-guiding-bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d6', '$.damageType', '光耀') WHERE id = 'spell-hunters-mark';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d10', '$.damageType', '穿刺') WHERE id = 'spell-hail-of-thorns';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d8', '$.damageType', '闪电') WHERE id = 'spell-chromatic-orb';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '2d8', '$.damageType', '力场') WHERE id = 'spell-chaos-bolt';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-entangle';
-- 旧格式法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_010_absorb_elements';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_012_bane';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_014_charm_person';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_015_command';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_017_fog_cloud';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_018_find_familiar';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell_020_identify';

-- 2环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-hold-person';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-invisibility';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-mirror-image';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-misty-step';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '1d8+2', '$.damageType', '力场') WHERE id = 'spell-spiritual-weapon';

-- 3环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '3d10', '$.damageType', '闪电') WHERE id = 'spell-call-lightning';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-counterspell';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '火焰') WHERE id = 'spell-fireball';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-fly';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d6', '$.damageType', '闪电') WHERE id = 'spell-lightning-bolt';

-- 4环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-greater-invisibility';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '5d8', '$.damageType', '寒冷') WHERE id = 'spell-ice-storm';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-polymorph';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-stoneskin';

-- 5环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '8d8', '$.damageType', '毒素') WHERE id = 'spell-cloudkill';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-raise-dead';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-scrying';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-wall-of-force';

-- 6环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '10d8', '$.damageType', '闪电') WHERE id = 'spell-chain-lightning';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '10d6+40', '$.damageType', '力场') WHERE id = 'spell-disintegrate';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-heal';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-true-seeing';

-- 7环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '12d6', '$.damageType', '火焰') WHERE id = 'spell-delayed-blast-fireball';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-plane-shift';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-resurrection';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-teleport';

-- 8环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-earthquake';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-mind-blank';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-power-word-stun';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '12d6', '$.damageType', '光耀') WHERE id = 'spell-sunburst';

-- 9环法术
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '20d6', '$.damageType', '火焰/钝击') WHERE id = 'spell-meteor-swarm';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-power-word-kill';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-time-stop';
UPDATE spell SET details = JSON_SET(IFNULL(details, '{}'), '$.damageDice', '0', '$.damageType', 'None') WHERE id = 'spell-wish';
