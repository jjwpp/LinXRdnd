package com.jjwpp.linxr.dm.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.entity.PlayerCharacter;
import com.jjwpp.linxr.entity.Spell;
import com.jjwpp.linxr.service.IPlayerCharacterService;
import com.jjwpp.linxr.service.ISpellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 法术工具 — 法术验证与施法。
 * <p>
 * 规则：
 * - 玩家只能施放已学习的法术（player_character.spell_ids）
 * - 1环以上法术需要消耗法术位
 * - 法术位不足时施法失败
 * - 部分法术自动命中（如魔法飞弹），其他需攻击检定
 * - 多目标法术（如火球术、燃烧之手）同时打击所有活着的敌人
 * <p>
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class SpellTool {

    @Autowired
    private ISpellService spellService;

    @Autowired
    private IPlayerCharacterService characterService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 法术验证结果
     */
    public static class SpellValidation {
        public boolean success;       // 是否可施放
        public String reason;         // 失败原因
        public Spell spell;           // 法术实体
        public int slotLevelUsed;     // 消耗的法术位环阶
    }

    /**
     * 验证角色是否可以施放指定法术
     * 检查项：
     * 1. 角色是否拥有该法术
     * 2. 法术环阶 > 0 时是否有可用法术位
     *
     * @param state    冒险状态
     * @param spellName 法术名称（从玩家行动中解析）
     * @return SpellValidation
     */
    @SuppressWarnings("unchecked")
    public SpellValidation validateSpell(AdventureState state, String spellName) {
        SpellValidation result = new SpellValidation();

        // 1. 从角色已学法术中查找
        try {
            PlayerCharacter pc = characterService.getById(state.getCharacterId());
            if (pc == null || pc.getSpellIds() == null) {
                result.success = false;
                result.reason = "角色没有学习任何法术";
                return result;
            }

            List<String> spellIds = mapper.readValue(pc.getSpellIds(), List.class);
            Spell matched = null;
            for (String id : spellIds) {
                Spell spell = spellService.getById(id);
                if (spell != null && spellName.contains(spell.getName())) {
                    matched = spell;
                    break;
                }
            }

            if (matched == null) {
                result.success = false;
                result.reason = "角色未学习法术「" + spellName + "」，无法施放";
                return result;
            }

            result.spell = matched;

            // 2. 戏法（0环）不需要法术位
            int spellLevel = matched.getLevel() != null ? matched.getLevel() : 0;
            if (spellLevel == 0) {
                result.success = true;
                result.slotLevelUsed = 0;
                return result;
            }

            // 3. 检查法术位
            int slotLevel = findAvailableSlot(state, spellLevel);
            if (slotLevel == 0) {
                result.success = false;
                result.reason = spellLevel + "环法术位已耗尽，无法施放「" + matched.getName() + "」";
                return result;
            }

            result.success = true;
            result.slotLevelUsed = slotLevel;
            return result;

        } catch (Exception e) {
            result.success = false;
            result.reason = "法术验证异常: " + e.getMessage();
            return result;
        }
    }

    /**
     * 消耗法术位
     */
    public void consumeSlot(AdventureState state, int level) {
        Integer remaining = state.getSpellSlots().get(level);
        if (remaining != null && remaining > 0) {
            state.getSpellSlots().put(level, remaining - 1);
        }
    }

    /**
     * 查找可用的法术位（从指定环阶开始向上找）
     */
    public int findAvailableSlot(AdventureState state, int minLevel) {
        for (int lv = minLevel; lv <= 9; lv++) {
            Integer remaining = state.getSpellSlots().get(lv);
            if (remaining != null && remaining > 0) return lv;
        }
        return 0;
    }

    /**
     * 判断法术是否治疗类
     */
    public boolean isHealSpell(Spell spell) {
        if (spell == null) return false;
        String name = spell.getName();
        return name.contains("疗伤") || name.contains("治疗") || name.contains("医疗")
                || name.contains("治愈") || name.contains("恢复");
    }

    /**
     * 判断法术是否自动命中（无需攻击检定）
     */
    public boolean isAutoHitSpell(Spell spell) {
        if (spell == null) return false;
        String name = spell.getName();
        return name.contains("飞弹") || name.contains("魔法飞弹")
                || name.contains("圣焰") || name.contains("毒雾");
    }

    /**
     * 判断法术是否多目标（AoE 法术，同时打击所有敌人）
     * 检测逻辑：
     * 1. details JSON 中 targetType == "multi"
     * 2. tags 包含"范围"/"多目标"/"锥形"/"直线"
     * 3. range 字段包含"锥"/"立方"/"线"/"半径"/"尺"
     */
    @SuppressWarnings("unchecked")
    public boolean isMultiTargetSpell(Spell spell) {
        if (spell == null) return false;

        // 1. 检查 details JSON 的 targetType 字段
        try {
            if (spell.getDetails() != null && !spell.getDetails().isBlank()) {
                Map<String, Object> details = mapper.readValue(spell.getDetails(), Map.class);
                Object targetType = details.get("targetType");
                if ("multi".equals(targetType)) return true;
            }
        } catch (Exception ignored) {}

        // 2. 检查 tags
        if (spell.getTags() != null) {
            String tags = spell.getTags().toLowerCase();
            if (tags.contains("范围") || tags.contains("多目标") || tags.contains("锥形")
                    || tags.contains("直线") || tags.contains("区域") || tags.contains("aoe")) {
                return true;
            }
        }

        // 3. 检查 range 字段
        if (spell.getRange() != null) {
            String range = spell.getRange();
            if (range.contains("锥") || range.contains("立方") || range.contains("线")
                    || range.contains("半径") || range.contains("尺范围") || range.contains("球形")) {
                return true;
            }
        }

        return false;
    }
}
