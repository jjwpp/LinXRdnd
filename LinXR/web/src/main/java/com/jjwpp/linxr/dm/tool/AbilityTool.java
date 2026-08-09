package com.jjwpp.linxr.dm.tool;

import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.entity.PlayerCharacter;
import org.springframework.stereotype.Component;

/**
 * 属性工具 — DND 5e 六维属性相关计算的唯一入口。
 * <p>
 * 规则：
 * - 属性调整值 (Ability Modifier) = floor((score - 10) / 2)
 * - 熟练加值 (Proficiency) = 2 + (level - 1) / 4
 * - 施法属性按职业映射：Wizard→INT, Cleric/Druid/Ranger→WIS, Bard/Sorcerer/Warlock/Paladin→CHA
 * <p>
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class AbilityTool {

    /**
     * 计算属性调整值 = floor((score - 10) / 2)
     */
    public int getModifier(int score) {
        return (score - 10) / 2; // Java 整数除法自动向零取整，等价于 floor 对正数
    }

    /**
     * 计算熟练加值 = 2 + (level - 1) / 4
     * L1-4: +2, L5-8: +3, L9-12: +4, L13-16: +5
     */
    public int getProficiency(int level) {
        return 2 + (level - 1) / 4;
    }

    /**
     * 从 PlayerCharacter 实体初始化 AdventureState 的六维属性和调整值
     */
    public void initAbilityScores(AdventureState state, PlayerCharacter pc) {
        int str = pc.getStrength() != null ? pc.getStrength() : 10;
        int dex = pc.getDexterity() != null ? pc.getDexterity() : 10;
        int con = pc.getConstitution() != null ? pc.getConstitution() : 10;
        int int_ = pc.getIntelligence() != null ? pc.getIntelligence() : 10;
        int wis = pc.getWisdom() != null ? pc.getWisdom() : 10;
        int cha = pc.getCharisma() != null ? pc.getCharisma() : 10;

        state.setStrength(str);
        state.setDexterity(dex);
        state.setConstitution(con);
        state.setIntelligence(int_);
        state.setWisdom(wis);
        state.setCharisma(cha);

        state.setStrMod(getModifier(str));
        state.setDexMod(getModifier(dex));
        state.setConMod(getModifier(con));
        state.setIntMod(getModifier(int_));
        state.setWisMod(getModifier(wis));
        state.setChaMod(getModifier(cha));
    }

    /**
     * 获取施法属性调整值（按职业映射）
     * Wizard → INT
     * Cleric, Druid, Ranger → WIS
     * Bard, Sorcerer, Warlock, Paladin → CHA
     */
    public int getSpellcastingMod(AdventureState state) {
        String classId = getSpellcastingAbility(state);
        return switch (classId) {
            case "int" -> state.getIntMod();
            case "wis" -> state.getWisMod();
            case "cha" -> state.getChaMod();
            default -> state.getIntMod();
        };
    }

    /**
     * 获取施法属性标识（用于 Prompt 展示）
     */
    public String getSpellcastingAbility(AdventureState state) {
        String className = state.getClassName();
        if (className == null) return "int";

        String n = className.toLowerCase();
        // Wizard → INT
        if (n.contains("法师") || n.contains("wizard") || n.contains("法")) return "int";
        // Cleric, Druid, Ranger → WIS
        if (n.contains("牧师") || n.contains("cleric") || n.contains("德鲁伊") || n.contains("druid")
                || n.contains("游侠") || n.contains("rang")) return "wis";
        // Bard, Sorcerer, Warlock, Paladin → CHA
        if (n.contains("吟游") || n.contains("bard") || n.contains("术士") || n.contains("sorcer")
                || n.contains("邪术") || n.contains("warlock") || n.contains("圣") || n.contains("palad")) return "cha";
        // 默认 INT
        return "int";
    }

    /**
     * 获取施法属性的中文标签
     */
    public String getSpellcastingAbilityLabel(AdventureState state) {
        return switch (getSpellcastingAbility(state)) {
            case "int" -> "智力";
            case "wis" -> "感知";
            case "cha" -> "魅力";
            default -> "智力";
        };
    }

    /**
     * 格式化属性值用于 Prompt 展示
     */
    public String formatAbilityScores(AdventureState state) {
        return String.format("STR:%d(%+d) DEX:%d(%+d) CON:%d(%+d) INT:%d(%+d) WIS:%d(%+d) CHA:%d(%+d)",
                state.getStrength(), state.getStrMod(),
                state.getDexterity(), state.getDexMod(),
                state.getConstitution(), state.getConMod(),
                state.getIntelligence(), state.getIntMod(),
                state.getWisdom(), state.getWisMod(),
                state.getCharisma(), state.getChaMod());
    }
}
