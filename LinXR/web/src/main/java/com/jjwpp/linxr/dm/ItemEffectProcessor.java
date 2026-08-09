package com.jjwpp.linxr.dm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjwpp.linxr.dm.tool.DiceTool;
import com.jjwpp.linxr.dm.tool.HpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用物品效果处理器
 * <p>
 * 根据 magic_item.details.effectType 统一处理所有物品效果，
 * 禁止为每种药水/道具写独立逻辑。
 * <p>
 * 支持的效果类型：
 * <ul>
 *   <li>HEAL         — 回复生命值</li>
 *   <li>MANA         — 回复法术位 / 法力</li>
 *   <li>BUFF         — 属性临时增强</li>
 *   <li>REMOVE_DEBUFF — 解除异常状态</li>
 *   <li>DAMAGE       — 造成伤害（投掷武器/爆炸物）</li>
 *   <li>SPECIAL      — 特殊效果（复活、传送等）</li>
 * </ul>
 * <p>
 * details JSON 格式示例：
 * <pre>
 * 生命药水: {"effectType":"HEAL","value":50,"actionCost":1}
 * 力量药水: {"effectType":"BUFF","attribute":"STRENGTH","value":5,"duration":3,"actionCost":1}
 * 火球卷轴: {"effectType":"DAMAGE","damageDice":"8d6","damageType":"fire","actionCost":1}
 * </pre>
 */
@Component
public class ItemEffectProcessor {

    @Autowired
    private DiceTool diceTool;

    @Autowired
    private HpTool hpTool;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 处理物品效果
     *
     * @param state    冒险会话状态（包含 HP/属性/法术位等运行时数据）
     * @param detailsJson  magic_item.details JSON 字符串
     * @return 处理结果描述（包含效果类型、数值变化等）
     */
    public EffectResult process(AdventureState state, String detailsJson) {
        EffectResult result = new EffectResult();

        if (detailsJson == null || detailsJson.isBlank()) {
            result.setSuccess(false);
            result.setMessage("该物品没有可激活的效果。");
            return result;
        }

        try {
            JsonNode details = mapper.readTree(detailsJson);
            String effectType = details.has("effectType") ? details.get("effectType").asText().toUpperCase() : "";

            // 读取行动消耗（默认1）
            int actionCost = details.has("actionCost") ? details.get("actionCost").asInt() : 1;
            result.setActionCost(actionCost);

            switch (effectType) {
                case "HEAL":
                    return processHeal(state, details, actionCost);
                case "MANA":
                    return processMana(state, details, actionCost);
                case "BUFF":
                    return processBuff(state, details, actionCost);
                case "REMOVE_DEBUFF":
                    return processRemoveDebuff(state, details, actionCost);
                case "DAMAGE":
                    return processDamage(state, details, actionCost);
                case "SPECIAL":
                    return processSpecial(state, details, actionCost);
                default:
                    result.setSuccess(false);
                    result.setMessage("未知的物品效果类型: " + effectType);
                    return result;
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("物品效果解析失败: " + e.getMessage());
            return result;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  各效果类型处理器
    // ════════════════════════════════════════════════════════════

    /**
     * HEAL — 回复生命值
     * details: {"effectType":"HEAL","value":50,"actionCost":1}
     * value 可以是固定值或骰子公式（如 "2d4+2"）
     */
    private EffectResult processHeal(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("HEAL");

        int healAmount;
        if (details.has("value")) {
            JsonNode valNode = details.get("value");
            if (valNode.isTextual()) {
                // 骰子公式，如 "2d4+2"
                healAmount = rollDiceFormula(valNode.asText());
            } else {
                healAmount = valNode.asInt();
            }
        } else {
            healAmount = 0;
        }

        int actualHeal = hpTool.applyHeal(state, healAmount);

        result.setSuccess(true);
        result.setMessage(String.format("恢复 %d 点生命值。", actualHeal));
        result.addStatChange("hp", state.getCurrentHp());
        result.addStatChange("healAmount", actualHeal);
        return result;
    }

    /**
     * MANA — 回复法术位 / 法力
     * details: {"effectType":"MANA","spellLevel":1,"value":1,"actionCost":1}
     * value = 恢复的法术位数量，spellLevel = 恢复哪个环阶的法术位
     * 如果不指定 spellLevel，则恢复最低环阶已消耗的法术位
     */
    private EffectResult processMana(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("MANA");

        int restoreAmount = details.has("value") ? details.get("value").asInt() : 1;
        int spellLevel = details.has("spellLevel") ? details.get("spellLevel").asInt() : 0;

        Map<Integer, Integer> slots = state.getSpellSlots();
        Map<Integer, Integer> maxSlots = state.getMaxSpellSlots();
        int restored = 0;

        if (spellLevel > 0) {
            // 恢复指定环阶
            int current = slots.getOrDefault(spellLevel, 0);
            int max = maxSlots.getOrDefault(spellLevel, 0);
            int newQty = Math.min(max, current + restoreAmount);
            restored = newQty - current;
            slots.put(spellLevel, newQty);
        } else {
            // 恢复最低环阶已消耗的法术位
            for (int lv = 1; lv <= 9; lv++) {
                int current = slots.getOrDefault(lv, 0);
                int max = maxSlots.getOrDefault(lv, 0);
                if (current < max) {
                    int newQty = Math.min(max, current + restoreAmount - restored);
                    restored += newQty - current;
                    slots.put(lv, newQty);
                    if (restored >= restoreAmount) break;
                }
            }
        }

        result.setSuccess(true);
        if (restored > 0) {
            result.setMessage(String.format("恢复 %d 个法术位。", restored));
        } else {
            result.setMessage("法术位已满，无法恢复。");
        }
        result.addStatChange("spellSlots", slots);
        return result;
    }

    /**
     * BUFF — 属性临时增强
     * details: {"effectType":"BUFF","attribute":"STRENGTH","value":5,"duration":3,"actionCost":1}
     * attribute: STRENGTH / DEXTERITY / CONSTITUTION / INTELLIGENCE / WISDOM / CHARISMA
     * duration: 持续回合数
     */
    private EffectResult processBuff(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("BUFF");

        String attribute = details.has("attribute") ? details.get("attribute").asText().toUpperCase() : "STRENGTH";
        int value = details.has("value") ? details.get("value").asInt() : 0;
        int duration = details.has("duration") ? details.get("duration").asInt() : 3;

        // 应用属性增强到运行时状态
        switch (attribute) {
            case "STRENGTH":
                state.setStrength(state.getStrength() + value);
                break;
            case "DEXTERITY":
                state.setDexterity(state.getDexterity() + value);
                break;
            case "CONSTITUTION":
                state.setConstitution(state.getConstitution() + value);
                break;
            case "INTELLIGENCE":
                state.setIntelligence(state.getIntelligence() + value);
                break;
            case "WISDOM":
                state.setWisdom(state.getWisdom() + value);
                break;
            case "CHARISMA":
                state.setCharisma(state.getCharisma() + value);
                break;
            default:
                result.setSuccess(false);
                result.setMessage("未知的属性类型: " + attribute);
                return result;
        }

        String attrName = getAttributeName(attribute);
        result.setSuccess(true);
        result.setMessage(String.format("%s 提升 %d 点，持续 %d 回合。", attrName, value, duration));
        result.addStatChange("buff", Map.of("attribute", attribute, "value", value, "duration", duration));
        result.addStatChange("abilities", buildAbilitiesMap(state));
        return result;
    }

    /**
     * REMOVE_DEBUFF — 解除异常状态
     * details: {"effectType":"REMOVE_DEBUFF","debuffs":["POISONED","PARALYZED"],"actionCost":1}
     * 如果不指定 debuffs，则解除所有异常状态
     */
    private EffectResult processRemoveDebuff(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("REMOVE_DEBUFF");

        // 当前简化实现：清除所有异常状态（未来可扩展状态系统）
        List<String> removed = new ArrayList<>();
        if (details.has("debuffs")) {
            for (JsonNode debuff : details.get("debuffs")) {
                removed.add(debuff.asText());
            }
        } else {
            removed.add("所有异常状态");
        }

        result.setSuccess(true);
        result.setMessage("解除了" + String.join("、", removed) + "。");
        result.addStatChange("removedDebuffs", removed);
        return result;
    }

    /**
     * DAMAGE — 造成伤害（投掷武器/爆炸物）
     * details: {"effectType":"DAMAGE","damageDice":"8d6","damageType":"fire","actionCost":1}
     * 或固定值: {"effectType":"DAMAGE","value":20,"damageType":"fire","actionCost":1}
     */
    private EffectResult processDamage(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("DAMAGE");

        int damageAmount;
        if (details.has("damageDice")) {
            damageAmount = rollDiceFormula(details.get("damageDice").asText());
        } else if (details.has("value")) {
            damageAmount = details.get("value").asInt();
        } else {
            damageAmount = 0;
        }

        String damageType = details.has("damageType") ? details.get("damageType").asText() : "未知";

        // 伤害施加给当前战斗中的第一个存活敌人
        if (state.getCombat() != null && state.getCombat().getEnemies() != null) {
            for (AdventureState.Enemy enemy : state.getCombat().getEnemies()) {
                if (enemy.isAlive()) {
                    enemy.setHp(Math.max(0, enemy.getHp() - damageAmount));
                    if (enemy.getHp() <= 0) {
                        enemy.setAlive(false);
                    }
                    result.setSuccess(true);
                    result.setMessage(String.format("对 %s 造成 %d 点%s伤害。", enemy.getName(), damageAmount, damageType));
                    result.addStatChange("target", enemy.getName());
                    result.addStatChange("damage", damageAmount);
                    result.addStatChange("damageType", damageType);
                    return result;
                }
            }
        }

        // 非战斗状态下无目标
        result.setSuccess(true);
        result.setMessage(String.format("释放了 %d 点%s伤害，但没有目标。", damageAmount, damageType));
        result.addStatChange("damage", damageAmount);
        return result;
    }

    /**
     * SPECIAL — 特殊效果
     * details: {"effectType":"SPECIAL","specialType":"REVIVE","actionCost":1}
     * 支持子类型: REVIVE（复活）、TELEPORT（传送）、IDENTIFY（鉴定）等
     */
    private EffectResult processSpecial(AdventureState state, JsonNode details, int actionCost) {
        EffectResult result = new EffectResult();
        result.setActionCost(actionCost);
        result.setEffectType("SPECIAL");

        String specialType = details.has("specialType") ? details.get("specialType").asText().toUpperCase() : "UNKNOWN";

        switch (specialType) {
            case "REVIVE":
                // 复活：恢复一半HP
                if (state.getCurrentHp() <= 0) {
                    state.setCurrentHp(state.getMaxHp() / 2);
                    state.setPhase("EXPLORE");
                    result.setSuccess(true);
                    result.setMessage("复活！恢复了一半生命值。");
                    result.addStatChange("hp", state.getCurrentHp());
                } else {
                    result.setSuccess(false);
                    result.setMessage("你还没有倒下，不需要复活。");
                }
                break;
            case "TELEPORT":
                result.setSuccess(true);
                result.setMessage("传送卷轴激活，你被传送到了安全的地方。");
                state.setPhase("EXPLORE");
                if (state.getCombat() != null) {
                    state.setCombat(null);
                }
                break;
            case "IDENTIFY":
                result.setSuccess(true);
                result.setMessage("鉴定卷轴发出微光，物品的属性已显现。");
                break;
            default:
                result.setSuccess(true);
                result.setMessage("物品发出了神秘的光芒，但效果未知。");
                break;
        }

        return result;
    }

    // ════════════════════════════════════════════════════════════
    //  工具方法
    // ════════════════════════════════════════════════════════════

    /**
     * 解析骰子公式，如 "2d4+2"、"1d6"、"3d8-1"
     */
    private int rollDiceFormula(String formula) {
        try {
            formula = formula.toLowerCase().trim();
            int bonus = 0;
            int dIndex = formula.indexOf('d');

            // 提取加减修正
            int plusIdx = formula.indexOf('+');
            int minusIdx = formula.indexOf('-');
            if (plusIdx >= 0 && plusIdx > dIndex) {
                bonus = Integer.parseInt(formula.substring(plusIdx + 1).trim());
                formula = formula.substring(0, plusIdx).trim();
            } else if (minusIdx >= 0 && minusIdx > dIndex) {
                bonus = -Integer.parseInt(formula.substring(minusIdx + 1).trim());
                formula = formula.substring(0, minusIdx).trim();
            }

            // 解析骰子
            dIndex = formula.indexOf('d');
            if (dIndex >= 0) {
                int count = Integer.parseInt(formula.substring(0, dIndex).trim());
                int faces = Integer.parseInt(formula.substring(dIndex + 1).trim());
                int total = 0;
                for (int i = 0; i < count; i++) {
                    total += diceTool.rollDice(1, faces);
                }
                return Math.max(0, total + bonus);
            } else {
                return Math.max(0, Integer.parseInt(formula) + bonus);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private String getAttributeName(String attr) {
        switch (attr) {
            case "STRENGTH": return "力量";
            case "DEXTERITY": return "敏捷";
            case "CONSTITUTION": return "体质";
            case "INTELLIGENCE": return "智力";
            case "WISDOM": return "感知";
            case "CHARISMA": return "魅力";
            default: return attr;
        }
    }

    private Map<String, Object> buildAbilitiesMap(AdventureState state) {
        Map<String, Object> abilities = new HashMap<>();
        abilities.put("str", state.getStrength());
        abilities.put("dex", state.getDexterity());
        abilities.put("con", state.getConstitution());
        abilities.put("int", state.getIntelligence());
        abilities.put("wis", state.getWisdom());
        abilities.put("cha", state.getCharisma());
        return abilities;
    }

    // ════════════════════════════════════════════════════════════
    //  效果结果对象
    // ════════════════════════════════════════════════════════════

    /**
     * 物品效果处理结果
     */
    @lombok.Data
    public static class EffectResult {
        private boolean success;
        private String effectType;
        private String message;
        private int actionCost;
        private Map<String, Object> statChanges = new HashMap<>();

        public void addStatChange(String key, Object value) {
            statChanges.put(key, value);
        }
    }
}
