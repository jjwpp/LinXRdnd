package com.jjwpp.linxr.dm.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.entity.Spell;
import com.jjwpp.linxr.entity.Weapon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 伤害计算工具 — 所有伤害数值的唯一来源。
 * <p>
 * 数据来源：
 * - 武器伤害 → weapon 表的 damage 字段（如 "1d8", "2d6"）+ 属性调整值
 * - 法术伤害 → spell 表的 details JSON 中 damageDice 字段（如 "3d4+3"）
 * - 怪物伤害 → monster 表的 damage_formula 字段（如 "1d6+2"）
 * <p>
 * 规则：
 * - 武器伤害 = 武器骰子 + 属性调整值（近战+STR_mod，远程+DEX_mod）
 * - 暴击时伤害骰子数量翻倍（不加固定加值/属性调整值额外翻倍）
 * - 所有伤害数值由 Tool 计算返回，AI 不得自行计算
 */
@Component
public class DamageTool {

    @Autowired
    private DiceTool diceTool;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 伤害计算结果
     */
    public static class DamageResult {
        public int damage;          // 最终伤害值
        public String damageType;   // 伤害类型
        public String formula;      // 使用的公式
        public boolean crit;        // 是否暴击
        public String description;  // 文字描述
    }

    /**
     * 从武器数据计算伤害（含属性调整值）
     *
     * @param weapon      武器实体（来自数据库）
     * @param crit        是否暴击
     * @param abilityMod  属性调整值（近战传 STR_mod，远程传 DEX_mod）
     * @return DamageResult
     */
    public DamageResult rollWeaponDamage(Weapon weapon, boolean crit, int abilityMod) {
        DamageResult result = new DamageResult();
        result.crit = crit;
        result.damageType = weapon.getDamageType() != null ? weapon.getDamageType() : "挥砍";

        String damageFormula = weapon.getDamage();
        if (damageFormula == null || damageFormula.isBlank()) {
            damageFormula = "1d6";
        }
        result.formula = damageFormula;

        int diceDamage;
        // 暴击：骰子数量翻倍
        if (crit) {
            diceDamage = rollCritFormula(damageFormula);
            result.description = "暴击伤害！" + damageFormula + " → " + diceDamage + " (+" + abilityMod + " 属性)";
        } else {
            diceDamage = diceTool.rollFormula(damageFormula);
            result.description = damageFormula + " → " + diceDamage + " (+" + abilityMod + " 属性)";
        }

        // 加上属性调整值（最低0，负数取0）
        result.damage = diceDamage + Math.max(0, abilityMod);

        return result;
    }

    /**
     * 从法术数据计算伤害
     *
     * @param spell 法术实体（来自数据库，details.damageDice）
     * @return DamageResult，如果法术无伤害则 damage=0
     */
    public DamageResult rollSpellDamage(Spell spell) {
        DamageResult result = new DamageResult();
        result.crit = false;

        // 从 details JSON 读取 damageDice 和 damageType
        String damageDice = null;
        String damageType = null;
        try {
            if (spell.getDetails() != null && !spell.getDetails().isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = mapper.readValue(spell.getDetails(), Map.class);
                damageDice = (String) details.get("damageDice");
                damageType = (String) details.get("damageType");
            }
        } catch (Exception ignored) {}

        if (damageDice == null || damageDice.isBlank() || "0".equals(damageDice)) {
            result.damage = 0;
            result.damageType = "None";
            result.formula = "0";
            result.description = "非伤害法术";
            return result;
        }

        result.formula = damageDice;
        result.damageType = damageType != null ? damageType : "魔法";
        result.damage = diceTool.rollFormula(damageDice);
        result.description = damageDice + " → " + result.damage;

        return result;
    }

    /**
     * 从怪物数据计算伤害
     *
     * @param damageFormula 怪物的伤害公式（来自 monster 表）
     * @param damageType    怪物的伤害类型（来自 monster 表）
     * @return DamageResult
     */
    public DamageResult rollMonsterDamage(String damageFormula, String damageType) {
        DamageResult result = new DamageResult();
        result.crit = false;

        if (damageFormula == null || damageFormula.isBlank()) {
            damageFormula = "1d6";
        }
        result.formula = damageFormula;
        result.damageType = damageType != null ? damageType : "物理";
        result.damage = diceTool.rollFormula(damageFormula);
        result.description = damageFormula + " → " + result.damage;

        return result;
    }

    /**
     * 暴击伤害：骰子数量翻倍，固定加值不变
     * 例如 "2d6+3" → "4d6+3"
     */
    private int rollCritFormula(String formula) {
        if (formula == null || formula.isBlank()) return 0;
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
            // 暴击：骰子翻倍
            return diceTool.rollDice(count * 2, sides) + modifier;
        }

        return modifier;
    }
}
