package com.jjwpp.linxr.dm.tool;

import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.entity.Monster;
import com.jjwpp.linxr.service.IMonsterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 遭遇生成工具 — 从怪物数据库生成战斗遭遇。
 * <p>
 * 规则：
 * - 所有敌人必须来自 monster 数据库表
 * - 怪物属性（HP/AC/攻击加值/伤害公式）直接从数据库读取，禁止凭空生成
 * - 根据玩家等级筛选合适CR范围的怪物
 * <p>
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class EncounterTool {

    @Autowired
    private IMonsterService monsterService;

    @Autowired
    private DiceTool diceTool;

    /**
     * 生成战斗遭遇
     *
     * @param playerLevel 玩家等级
     * @param location    当前位置（影响怪物选择，暂未深度使用）
     * @return 敌人列表（已从数据库读取属性）
     */
    public List<AdventureState.Enemy> generateEncounter(int playerLevel, String location) {
        List<Monster> allMonsters = monsterService.list();
        if (allMonsters == null || allMonsters.isEmpty()) {
            return new ArrayList<>();
        }

        // 按等级筛选合适 CR 的怪物
        double maxCr = Math.max(0.25, playerLevel * 0.8);
        double minCr = Math.max(0.125, playerLevel * 0.2);
        List<Monster> suitable = new ArrayList<>();
        for (Monster m : allMonsters) {
            double cr = parseCr(m.getCr());
            if (cr >= minCr && cr <= maxCr) {
                suitable.add(m);
            }
        }
        if (suitable.isEmpty()) {
            suitable = new ArrayList<>(allMonsters);
        }

        // 1‑3级:1只；4‑8级:1‑2只；9级以上:1‑3只
        Collections.shuffle(suitable);
        int maxCount;
        if (playerLevel <= 3) {
            maxCount = 1;
        } else if (playerLevel <= 8) {
            maxCount = 2;
        } else {
            maxCount = 3;
        }
        // nextInt(1, maxCount+1)：范围 [1, maxCount]
        int count = Math.min(suitable.size(), ThreadLocalRandom.current().nextInt(1, maxCount + 1));
        List<AdventureState.Enemy> enemies = new ArrayList<>();

        for (int i = 0; i < count && i < suitable.size(); i++) {
            Monster m = suitable.get(i);
            enemies.add(createEnemyFromMonster(m));
        }

        return enemies;
    }

    /**
     * 从数据库怪物实体创建战斗敌人（属性全部来自数据库）
     */
    public AdventureState.Enemy createEnemyFromMonster(Monster m) {
        int hp = parseMonsterHp(m.getHp());
        int ac = m.getAc() != null ? m.getAc() : 10;
        int xpReward = getXpForCr(m.getCr());
        int attackBonus = m.getAttackBonus() != null ? m.getAttackBonus() : 4;
        String damageFormula = m.getDamageFormula() != null ? m.getDamageFormula() : "1d6";
        String damageType = m.getDamageType() != null ? m.getDamageType() : "物理";

        AdventureState.Enemy enemy = new AdventureState.Enemy(m.getName(), hp, ac, xpReward);
        enemy.setMonsterId(m.getId());
        enemy.setImageUrl(m.getImageUrl());
        enemy.setAttackBonus(attackBonus);
        // 解析伤害公式为 Enemy 的骰子字段（向后兼容）
        parseDamageFormulaToEnemy(enemy, damageFormula);
        enemy.setDamageType(damageType);

        return enemy;
    }

    /**
     * 获取怪物的伤害公式（直接从数据库读取）
     */
    public String getMonsterDamageFormula(Monster m) {
        return m.getDamageFormula() != null ? m.getDamageFormula() : "1d6";
    }

    // ── 私有工具方法 ──

    private void parseDamageFormulaToEnemy(AdventureState.Enemy enemy, String formula) {
        if (formula == null || formula.isBlank()) {
            enemy.setDamageDice(6);
            enemy.setDamageCount(1);
            enemy.setDamageBonus(0);
            return;
        }
        formula = formula.trim();

        int modifier = 0;
        String dicePart = formula;

        int plusIdx = formula.lastIndexOf('+');
        int minusIdx = formula.lastIndexOf('-');

        if (plusIdx > 0) {
            dicePart = formula.substring(0, plusIdx).trim();
            modifier = Integer.parseInt(formula.substring(plusIdx + 1).trim());
        } else if (minusIdx > 0) {
            dicePart = formula.substring(0, minusIdx).trim();
            modifier = -Integer.parseInt(formula.substring(minusIdx + 1).trim());
        }

        if (dicePart.contains("d") || dicePart.contains("D")) {
            String[] parts = dicePart.split("[dD]");
            int count = parts[0].isEmpty() ? 1 : Integer.parseInt(parts[0].trim());
            int sides = Integer.parseInt(parts[1].trim());
            enemy.setDamageDice(sides);
            enemy.setDamageCount(count);
            enemy.setDamageBonus(modifier);
        } else {
            enemy.setDamageDice(6);
            enemy.setDamageCount(1);
            enemy.setDamageBonus(Integer.parseInt(dicePart));
        }
    }

    private int parseMonsterHp(String hpStr) {
        if (hpStr == null) return 10;
        int parenIdx = hpStr.indexOf('(');
        if (parenIdx > 0) {
            try {
                return Integer.parseInt(hpStr.substring(0, parenIdx).trim());
            } catch (NumberFormatException ignored) {}
        }
        try {
            return Integer.parseInt(hpStr.trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private double parseCr(String cr) {
        if (cr == null) return 0;
        if (cr.contains("/")) {
            String[] parts = cr.split("/");
            try {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } catch (Exception e) {
                return 0;
            }
        }
        try {
            return Double.parseDouble(cr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getXpForCr(String crStr) {
        double cr = parseCr(crStr);
        if (cr <= 0) return 20;
        if (cr <= 0.125) return 40;
        if (cr <= 0.25) return 80;
        if (cr <= 0.5) return 150;
        if (cr <= 1) return 300;
        if (cr <= 2) return 450;
        if (cr <= 3) return 700;
        if (cr <= 4) return 1100;
        if (cr <= 5) return 1800;
        if (cr <= 6) return 2300;
        if (cr <= 7) return 2900;
        if (cr <= 8) return 3900;
        if (cr <= 9) return 5000;
        if (cr <= 10) return 5900;
        if (cr <= 12) return 8400;
        return 10000;
    }
}
