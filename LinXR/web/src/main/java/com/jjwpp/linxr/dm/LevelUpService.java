package com.jjwpp.linxr.dm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjwpp.linxr.entity.*;
import com.jjwpp.linxr.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 角色升级服务
 * <p>
 * 负责从数据库读取职业成长配置、构建升级预览、处理玩家选择。
 * DmService 委托本类处理升级相关逻辑。
 */
@Slf4j
@Service
public class LevelUpService {

    @Autowired private IClassLevelProgressionService progressionService;
    @Autowired private ISpellService spellService;
    @Autowired private IFeatService featService;
    @Autowired private IPlayerCharacterService characterService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ════════════════════════════════════════════════════════════
    //  构建升级预览 (供前端展示)
    // ════════════════════════════════════════════════════════════

    /**
     * 构建升级选择数据
     * <p>
     * 从数据库读取职业成长配置，构建包含奖励列表和可选项的 LevelUpChoices。
     */
    @SuppressWarnings("unchecked")
    public AdventureState.LevelUpChoices buildLevelUpChoices(AdventureState state) {
        int newLevel = state.getLevel() + 1;
        AdventureState.LevelUpChoices choices = new AdventureState.LevelUpChoices();
        choices.setNewLevel(newLevel);
        choices.setClassName(state.getClassName());

        try {
            PlayerCharacter pc = characterService.getById(state.getCharacterId());
            if (pc == null) return choices;

            String classId = pc.getClassId();

            // 查询该等级的所有升级奖励
            List<ClassLevelProgression> progressions = progressionService.getByClassAndLevel(classId, newLevel);
            if (progressions == null || progressions.isEmpty()) {
                log.warn("[LevelUp] No progression data for class={} level={}", classId, newLevel);
                return choices;
            }

            // 已知法术/专长集合 (用于过滤)
            Set<String> knownSpellIds = new HashSet<>();
            if (pc.getSpellIds() != null) {
                knownSpellIds.addAll(mapper.readValue(pc.getSpellIds(), List.class));
            }
            Set<String> knownFeatIds = new HashSet<>();
            if (pc.getFeatIds() != null) {
                knownFeatIds.addAll(mapper.readValue(pc.getFeatIds(), List.class));
            }

            boolean needSpells = false;
            boolean needFeats = false;
            int maxSpellLevel = 0;

            // 构建奖励列表
            for (ClassLevelProgression prog : progressions) {
                AdventureState.LevelUpReward reward = new AdventureState.LevelUpReward();
                reward.setId(prog.getId());
                reward.setRewardType(prog.getRewardType());
                reward.setRewardName(prog.getRewardName());
                reward.setDescription(prog.getDescription());

                // 解析 reward_data JSON
                Map<String, Object> data = null;
                if (prog.getRewardData() != null && !prog.getRewardData().isBlank()) {
                    try {
                        data = mapper.readValue(prog.getRewardData(), Map.class);
                    } catch (Exception e) {
                        log.warn("[LevelUp] Failed to parse reward_data: {}", prog.getRewardData());
                    }
                }
                reward.setData(data);

                // 判断是否需要玩家选择
                String type = prog.getRewardType();
                boolean requiresChoice = "ASI".equals(type) || "NEW_SPELL".equals(type)
                        || "COMBAT_STYLE".equals(type) || "FEAT_CHOICE".equals(type);
                reward.setRequiresChoice(requiresChoice);
                choices.getRewards().add(reward);

                // 收集需要查询的法术/专长
                if ("NEW_SPELL".equals(type) && data != null) {
                    needSpells = true;
                    Object msl = data.get("maxSpellLevel");
                    if (msl != null) {
                        maxSpellLevel = Math.max(maxSpellLevel, ((Number) msl).intValue());
                    }
                }
                if ("ASI".equals(type) || "FEAT_CHOICE".equals(type)) {
                    needFeats = true;
                }
            }

            // ASI 选项
            choices.setAsiOptions(Arrays.asList("str+2", "dex+2", "con+2", "int+2", "wis+2", "cha+2", "feat"));

            // 查询可选法术
            if (needSpells) {
                choices.setAvailableSpells(queryAvailableSpells(classId, maxSpellLevel, knownSpellIds));
            }

            // 查询可选专长
            if (needFeats) {
                choices.setAvailableFeats(queryAvailableFeats(knownFeatIds));
            }

        } catch (Exception e) {
            log.error("[LevelUp] buildLevelUpChoices error", e);
        }

        return choices;
    }

    /**
     * 获取升级信息 (GET 接口返回数据)
     */
    public Map<String, Object> getLevelUpInfo(AdventureState state) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("characterName", state.getCharacterName());
        info.put("className", state.getClassName());
        info.put("currentLevel", state.getLevel());

        AdventureState.LevelUpChoices choices = state.getLevelUpChoices();
        if (choices == null) {
            choices = buildLevelUpChoices(state);
        }
        info.put("newLevel", choices.getNewLevel());
        info.put("rewards", choices.getRewards());
        info.put("availableSpells", choices.getAvailableSpells());
        info.put("availableFeats", choices.getAvailableFeats());
        info.put("asiOptions", choices.getAsiOptions());

        // HP 预览
        Map<String, Object> hpPreview = new LinkedHashMap<>();
        hpPreview.put("currentMaxHp", state.getMaxHp());
        info.put("hpPreview", hpPreview);

        // 法术位预览
        info.put("currentSpellSlots", state.getMaxSpellSlots());

        return info;
    }

    // ════════════════════════════════════════════════════════════
    //  处理玩家选择
    // ════════════════════════════════════════════════════════════

    /**
     * 处理升级奖励，更新 PlayerCharacter 数据库记录
     * <p>
     * 返回获得的奖励描述列表 (供 AI 叙事使用)。
     *
     * @param state   冒险状态 (会更新属性值)
     * @param choices 玩家选择 { asi: {stat, amount} | {featId}, spellId, combatStyle, featId }
     * @return 奖励描述列表
     */
    @SuppressWarnings("unchecked")
    public List<String> processLevelUpRewards(AdventureState state, Map<String, Object> choices) {
        List<String> acquired = new ArrayList<>();
        int newLevel = state.getLevel() + 1;

        try {
            PlayerCharacter pc = characterService.getById(state.getCharacterId());
            if (pc == null) return acquired;

            String classId = pc.getClassId();
            List<ClassLevelProgression> progressions = progressionService.getByClassAndLevel(classId, newLevel);

            if (progressions == null || progressions.isEmpty()) {
                log.warn("[LevelUp] No progression data for class={} level={}", classId, newLevel);
                return acquired;
            }

            // 解析玩家选择
            Map<String, Object> asiChoice = choices != null && choices.containsKey("asi")
                    ? (Map<String, Object>) choices.get("asi") : null;
            String spellId = choices != null && choices.containsKey("spellId")
                    ? (String) choices.get("spellId") : null;
            String combatStyle = choices != null && choices.containsKey("combatStyle")
                    ? (String) choices.get("combatStyle") : null;
            String featId = choices != null && choices.containsKey("featId")
                    ? (String) choices.get("featId") : null;

            // 用于持久化到 PlayerCharacter 的 spell/feat 列表
            List<String> pcSpellIds = pc.getSpellIds() != null
                    ? mapper.readValue(pc.getSpellIds(), List.class) : new ArrayList<String>();
            List<String> pcFeatIds = pc.getFeatIds() != null
                    ? mapper.readValue(pc.getFeatIds(), List.class) : new ArrayList<String>();

            for (ClassLevelProgression prog : progressions) {
                String type = prog.getRewardType();
                String name = prog.getRewardName();

                switch (type) {
                    case "ABILITY":
                        // 自动获得，记录描述
                        acquired.add("【能力】" + name + ": " + prog.getDescription());
                        break;

                    case "ASI":
                        // 属性提升
                        if (asiChoice != null) {
                            String asiType = (String) asiChoice.get("type");
                            if ("stat".equals(asiType)) {
                                String stat = (String) asiChoice.get("stat");
                                int amount = asiChoice.containsKey("amount")
                                        ? ((Number) asiChoice.get("amount")).intValue() : 2;
                                applyStatIncrease(state, pc, stat, amount);
                                acquired.add("【属性】" + statToName(stat) + " +" + amount);
                            } else if ("feat".equals(asiType)) {
                                String chosenFeatId = (String) asiChoice.get("featId");
                                if (chosenFeatId != null && !pcFeatIds.contains(chosenFeatId)) {
                                    pcFeatIds.add(chosenFeatId);
                                    Feat feat = featService.getById(chosenFeatId);
                                    if (feat != null) {
                                        acquired.add("【专长】" + feat.getName() + ": " + feat.getSummary());
                                    }
                                }
                            }
                        }
                        break;

                    case "NEW_SPELL":
                        // 学习新法术
                        if (spellId != null && !pcSpellIds.contains(spellId)) {
                            pcSpellIds.add(spellId);
                            Spell spell = spellService.getById(spellId);
                            if (spell != null) {
                                acquired.add("【法术】" + spell.getName() + ": " + spell.getSummary());
                            }
                        }
                        break;

                    case "COMBAT_STYLE":
                        // 战斗风格选择
                        if (combatStyle != null) {
                            acquired.add("【战斗风格】" + combatStyleToName(combatStyle));
                            // 将战斗风格记录到 details JSON 中
                            recordCombatStyle(pc, combatStyle);
                        }
                        break;

                    case "FEAT_CHOICE":
                        // 专长选择
                        if (featId != null && !pcFeatIds.contains(featId)) {
                            pcFeatIds.add(featId);
                            Feat feat = featService.getById(featId);
                            if (feat != null) {
                                acquired.add("【专长】" + feat.getName() + ": " + feat.getSummary());
                            }
                        }
                        break;
                }
            }

            // 持久化 spells/feats 到 PlayerCharacter
            pc.setSpellIds(mapper.writeValueAsString(pcSpellIds));
            pc.setFeatIds(mapper.writeValueAsString(pcFeatIds));

            // 更新等级
            pc.setLevel(newLevel);

            // 更新六维属性 (如果 ASI 修改了)
            pc.setStrength(state.getStrength());
            pc.setDexterity(state.getDexterity());
            pc.setConstitution(state.getConstitution());
            pc.setIntelligence(state.getIntelligence());
            pc.setWisdom(state.getWisdom());
            pc.setCharisma(state.getCharisma());

            characterService.updateById(pc);

        } catch (Exception e) {
            log.error("[LevelUp] processLevelUpRewards error", e);
        }

        return acquired;
    }

    // ════════════════════════════════════════════════════════════
    //  私有工具方法
    // ════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private List<AdventureState.Choice> queryAvailableSpells(String classId, int maxSpellLevel, Set<String> knownSpellIds) {
        List<AdventureState.Choice> result = new ArrayList<>();
        List<Spell> allSpells = spellService.list();
        for (Spell s : allSpells) {
            int sl = s.getLevel() != null ? s.getLevel() : 0;
            if (sl > 0 && sl <= maxSpellLevel && !knownSpellIds.contains(s.getId()) && spellMatchesClass(s, classId)) {
                result.add(new AdventureState.Choice(s.getId(), s.getName(), s.getSummary()));
                if (result.size() >= 8) break;
            }
        }
        return result;
    }

    private List<AdventureState.Choice> queryAvailableFeats(Set<String> knownFeatIds) {
        List<AdventureState.Choice> result = new ArrayList<>();
        List<Feat> allFeats = featService.list();
        for (Feat f : allFeats) {
            if (!knownFeatIds.contains(f.getId())) {
                result.add(new AdventureState.Choice(f.getId(), f.getName(), f.getSummary()));
                if (result.size() >= 6) break;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private boolean spellMatchesClass(Spell spell, String classId) {
        if (classId == null || classId.isBlank()) return true;
        String classIdsJson = spell.getClassIds();
        if (classIdsJson == null || classIdsJson.isBlank()) return true;
        try {
            List<String> ids = mapper.readValue(classIdsJson, List.class);
            return ids.contains(classId);
        } catch (Exception e) {
            return true;
        }
    }

    private void applyStatIncrease(AdventureState state, PlayerCharacter pc, String stat, int amount) {
        switch (stat) {
            case "str":
                state.setStrength(state.getStrength() + amount);
                state.setStrMod((state.getStrength() - 10) / 2);
                break;
            case "dex":
                state.setDexterity(state.getDexterity() + amount);
                state.setDexMod((state.getDexterity() - 10) / 2);
                break;
            case "con":
                state.setConstitution(state.getConstitution() + amount);
                state.setConMod((state.getConstitution() - 10) / 2);
                break;
            case "int":
                state.setIntelligence(state.getIntelligence() + amount);
                state.setIntMod((state.getIntelligence() - 10) / 2);
                break;
            case "wis":
                state.setWisdom(state.getWisdom() + amount);
                state.setWisMod((state.getWisdom() - 10) / 2);
                break;
            case "cha":
                state.setCharisma(state.getCharisma() + amount);
                state.setChaMod((state.getCharisma() - 10) / 2);
                break;
        }
    }

    private String statToName(String stat) {
        switch (stat) {
            case "str": return "力量";
            case "dex": return "敏捷";
            case "con": return "体质";
            case "int": return "智力";
            case "wis": return "感知";
            case "cha": return "魅力";
            default: return stat;
        }
    }

    private String combatStyleToName(String style) {
        switch (style) {
            case "defense": return "防御 (AC+1)";
            case "two_weapon": return "双武器 (副手强化)";
            case "archery": return "弓术 (远程命中+2)";
            case "great_weapon": return "重武器 (伤害增加)";
            default: return style;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordCombatStyle(PlayerCharacter pc, String combatStyle) {
        try {
            Map<String, Object> details = new HashMap<>();
            if (pc.getDetails() != null && !pc.getDetails().isBlank()) {
                details = mapper.readValue(pc.getDetails(), Map.class);
            }
            details.put("combatStyle", combatStyle);
            pc.setDetails(mapper.writeValueAsString(details));
        } catch (Exception e) {
            log.warn("[LevelUp] Failed to record combat style", e);
        }
    }
}
