package com.jjwpp.linxr.dm.tool;

import com.jjwpp.linxr.dm.AdventureState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 攻击检定工具 — 处理所有"是否命中"的判定。
 * <p>
 * 规则：
 * - 攻击检定 = d20 + 攻击加值
 * - 近战攻击加值 = 熟练加值 + STR_mod
 * - 远程攻击加值 = 熟练加值 + DEX_mod
 * - 法术攻击加值 = 熟练加值 + 施法属性调整值
 * - 如果结果 >= 目标AC → 命中
 * - 自然20 → 暴击（必定命中，伤害骰翻倍）
 * - 自然1 → 失误（必定未命中）
 * <p>
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class AttackTool {

    @Autowired
    private DiceTool diceTool;

    @Autowired
    private AbilityTool abilityTool;

    /**
     * 攻击检定结果
     */
    public static class AttackResult {
        public boolean hit;          // 是否命中
        public boolean crit;         // 是否暴击
        public boolean fumble;       // 是否失误
        public int roll;             // d20原始值
        public int totalAttack;      // d20 + 加值
        public int targetAc;         // 目标AC
        public String description;   // 文字描述
    }

    /**
     * 执行一次攻击检定
     *
     * @param attackBonus 攻击加值（熟练+属性调整值）
     * @param targetAc    目标护甲等级
     * @return AttackResult
     */
    public AttackResult resolveAttack(int attackBonus, int targetAc) {
        DiceTool.D20Result d20 = diceTool.rollD20WithDetail();
        AttackResult result = new AttackResult();
        result.roll = d20.roll;
        result.crit = d20.crit;
        result.fumble = d20.fumble;
        result.targetAc = targetAc;
        result.totalAttack = d20.roll + attackBonus;

        if (d20.crit) {
            result.hit = true;
            result.description = "暴击！自然20，必定命中";
        } else if (d20.fumble) {
            result.hit = false;
            result.description = "失误！自然1，必定未命中";
        } else {
            result.hit = result.totalAttack >= targetAc;
            if (result.hit) {
                result.description = "命中（" + result.totalAttack + " vs AC" + targetAc + "）";
            } else {
                result.description = "未命中（" + result.totalAttack + " vs AC" + targetAc + "）";
            }
        }

        return result;
    }

    /**
     * 玩家近战攻击检定
     * 攻击加值 = 熟练加值 + STR_mod
     */
    public AttackResult playerMeleeAttack(AdventureState state, AdventureState.Enemy target) {
        int prof = abilityTool.getProficiency(state.getLevel());
        int attackBonus = prof + state.getStrMod();
        return resolveAttack(attackBonus, target.getAc());
    }

    /**
     * 玩家远程攻击检定
     * 攻击加值 = 熟练加值 + DEX_mod
     */
    public AttackResult playerRangedAttack(AdventureState state, AdventureState.Enemy target) {
        int prof = abilityTool.getProficiency(state.getLevel());
        int attackBonus = prof + state.getDexMod();
        return resolveAttack(attackBonus, target.getAc());
    }

    /**
     * 法术攻击检定
     * 攻击加值 = 熟练加值 + 施法属性调整值
     */
    public AttackResult playerSpellAttack(AdventureState state, AdventureState.Enemy target) {
        int prof = abilityTool.getProficiency(state.getLevel());
        int spellMod = abilityTool.getSpellcastingMod(state);
        int attackBonus = prof + spellMod;
        return resolveAttack(attackBonus, target.getAc());
    }

    /**
     * 敌人攻击检定
     */
    public AttackResult enemyAttack(AdventureState.Enemy enemy, int playerAc) {
        return resolveAttack(enemy.getAttackBonus(), playerAc);
    }
}
