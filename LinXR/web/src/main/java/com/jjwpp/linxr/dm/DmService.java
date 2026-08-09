package com.jjwpp.linxr.dm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjwpp.linxr.entity.Class;
import com.jjwpp.linxr.entity.Feat;
import com.jjwpp.linxr.entity.Monster;
import com.jjwpp.linxr.entity.PlayerCharacter;
import com.jjwpp.linxr.entity.Race;
import com.jjwpp.linxr.entity.Spell;
import com.jjwpp.linxr.entity.Weapon;
import com.jjwpp.linxr.entity.CharacterInventory;
import com.jjwpp.linxr.entity.MagicItem;
import com.jjwpp.linxr.entity.MonsterDrop;
import com.jjwpp.linxr.service.IClassService;
import com.jjwpp.linxr.service.IFeatService;
import com.jjwpp.linxr.service.IMonsterService;
import com.jjwpp.linxr.service.IPlayerCharacterService;
import com.jjwpp.linxr.service.IRaceService;
import com.jjwpp.linxr.service.ISpellService;
import com.jjwpp.linxr.service.IWeaponService;
import com.jjwpp.linxr.service.IArmorService;
import com.jjwpp.linxr.service.ICharacterInventoryService;
import com.jjwpp.linxr.service.IMagicItemService;
import com.jjwpp.linxr.service.IMonsterDropService;
import com.jjwpp.linxr.dm.tool.AbilityTool;
import com.jjwpp.linxr.dm.tool.AttackTool;
import com.jjwpp.linxr.dm.tool.DamageTool;
import com.jjwpp.linxr.dm.tool.DiceTool;
import com.jjwpp.linxr.dm.tool.EncounterTool;
import com.jjwpp.linxr.dm.tool.HpTool;
import com.jjwpp.linxr.dm.tool.LootTool;
import com.jjwpp.linxr.dm.tool.SpellTool;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * AI 地下城主服务
 * <p>
 * 核心原则：AI 负责创意（旁白/场景），代码负责确定性数据（HP/骰子/XP/法术位）。
 * 流式输出通过 SseEmitter 推送到前端，逐字渲染旁白。
 */
@Service
public class DmService {

    private static final Logger log = LoggerFactory.getLogger(DmService.class);

    @Autowired
    private StreamingChatModel streamingModel;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IPlayerCharacterService characterService;

    @Autowired
    private IRaceService raceService;

    @Autowired
    private IClassService classService;

    @Autowired
    private ISpellService spellService;

    @Autowired
    private IMonsterService monsterService;

    @Autowired
    private IFeatService featService;

    // ═══ 游戏规则 Tool 类（所有数值计算通过 Tool 完成，AI 不直接计算） ═══

    @Autowired
    private DiceTool diceTool;

    @Autowired
    private AttackTool attackTool;

    @Autowired
    private DamageTool damageTool;

    @Autowired
    private SpellTool spellTool;

    @Autowired
    private EncounterTool encounterTool;

    @Autowired
    private LootTool lootTool;

    @Autowired
    private HpTool hpTool;

    @Autowired
    private IWeaponService weaponService;

    @Autowired
    private IArmorService armorService;

    @Autowired
    private ICharacterInventoryService inventoryService;

    @Autowired
    private IMagicItemService magicItemService;

    @Autowired
    private IMonsterDropService monsterDropService;

    @Autowired
    private ItemEffectProcessor itemEffectProcessor;

    @Autowired
    private AbilityTool abilityTool;

    @Autowired
    private LevelUpService levelUpService;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Prompt 模板缓存：从 classpath:prompts/*.txt 加载，首次访问时懒加载 */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private static final String REDIS_KEY_PREFIX = "adventure:session:";
    private static final long SESSION_TTL_HOURS = 4;
    private static final int MAX_HISTORY = 8;

    // 简化升级阈值（索引=当前等级，值=升到下一级所需XP）
    // 比标准DND 5e大幅降低前5级门槛，让初期升级更快
    private static final int[] XP_THRESHOLDS = {
            0, 120, 350, 900, 2200, 5000, 12000, 23000, 34000, 48000, 64000,
            85000, 100000, 120000, 153000, 190000
    };

    // DND 5e 法术位表 [等级][环阶1-9]
    private static final int[][] SPELL_SLOT_TABLE = {
            {2, 0, 0, 0, 0, 0, 0, 0, 0},   // L1
            {3, 0, 0, 0, 0, 0, 0, 0, 0},   // L2
            {4, 2, 0, 0, 0, 0, 0, 0, 0},   // L3
            {4, 3, 0, 0, 0, 0, 0, 0, 0},   // L4
            {4, 3, 2, 0, 0, 0, 0, 0, 0},   // L5
            {4, 3, 3, 0, 0, 0, 0, 0, 0},   // L6
            {4, 3, 3, 1, 0, 0, 0, 0, 0},   // L7
            {4, 3, 3, 2, 0, 0, 0, 0, 0},   // L8
            {4, 3, 3, 3, 1, 0, 0, 0, 0},   // L9
            {4, 3, 3, 3, 2, 0, 0, 0, 0},   // L10
            {4, 3, 3, 3, 2, 1, 0, 0, 0},   // L11
            {4, 3, 3, 3, 2, 1, 0, 0, 0},   // L12
    };

    // ════════════════════════════════════════════════════════════
    //  公开方法
    // ════════════════════════════════════════════════════════════

    /**
     * 开始一场新冒险：创建会话状态，初始化 HP/法术位/XP，生成开场旁白
     */
    public void startAdventure(String characterId, SseEmitter emitter) {
        try {
            PlayerCharacter pc = characterService.getById(characterId);
            if (pc == null) {
                sendError(emitter, "角色不存在: " + characterId);
                return;
            }

            String raceName = resolveName(pc.getRaceId(), raceService);
            String className = resolveName(pc.getClassId(), classService);

            String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            AdventureState state = new AdventureState();
            state.setSessionId(sessionId);
            state.setCharacterId(characterId);
            state.setCharacterName(pc.getName());
            state.setRaceName(raceName);
            state.setClassName(className);
            int level = pc.getLevel() != null ? pc.getLevel() : 1;
            state.setLevel(level);
            state.setSummary(pc.getSummary() != null ? pc.getSummary() : "一位即将踏上旅途的冒险者");
            state.setLocation("未知之地");

            // 初始化运行时状态
            state.setPhase("EXPLORE");
            // 初始化六维属性 + 调整值
            abilityTool.initAbilityScores(state, pc);
            initStats(state, level, className, pc);
            state.setSpells(resolveSpells(pc.getSpellIds()));
            state.setInventory(loadInventory(characterId));
            state.setXp(0);
            state.setXpToNext(getXpToNext(level));
            state.setExploreTurnCount(0);

            saveState(state);

            emitter.send(SseEmitter.event().name("session").data(sessionId));

            log.info("[Agent] 开始新冒险: session={} character={}", sessionId, state.getCharacterName());

            // 使用 Agent 生成开场旁白
            streamAgentResponse(state, loadPrompt("adventure_start"), emitter);

        } catch (Exception e) {
            sendError(emitter, "启动冒险失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家行动 — Agent 模式。
     * <p>
     * AI 自主决定调用哪些游戏工具（攻击、施法、使用物品等），
     * 工具执行结果返回给 AI，AI 基于结果生成叙事。
     * 不再由 Java 代码解析玩家意图和路由游戏流程。
     */
    public void processAction(String sessionId, String action, SseEmitter emitter) {
        try {
            AdventureState state = loadState(sessionId);
            if (state == null) {
                sendError(emitter, "会话不存在或已过期，请重新开始冒险");
                return;
            }

            if ("DEAD".equals(state.getPhase())) {
                sendError(emitter, "角色已死亡，冒险结束");
                return;
            }

            if ("LEVELUP".equals(state.getPhase())) {
                sendError(emitter, "请先完成升级选择");
                return;
            }

            // ENCOUNTER 阶段：等待玩家在前端点击"进入战斗"按钮，不处理自由文本行动
            if ("ENCOUNTER".equals(state.getPhase())) {
                // 如果玩家在遭遇弹窗中输入了文本，自动确认进入战斗
                confirmEncounterAndStream(state, emitter);
                return;
            }

            log.info("[Agent] session={} action={} phase={}", sessionId, action, state.getPhase());

            // 使用 Agent 模式：AI 自主决策调用工具
            streamAgentResponse(state, action, emitter);

        } catch (Exception e) {
            sendError(emitter, "处理行动失败: " + e.getMessage());
        }
    }

    /**
     * 长休：恢复全部 HP 和法术位，重置生命骰
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> longRest(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        state.setCurrentHp(state.getMaxHp());
        state.setSpellSlots(new HashMap<>(state.getMaxSpellSlots()));
        // 长休恢复一半生命骰（至少1）
        state.setHitDice(Math.max(1, state.getMaxHitDice() / 2));
        state.setPhase("EXPLORE");
        saveState(state);

        return buildDonePayload(state, "你找了一处安全的地方长休，体力完全恢复，法术位已充满，生命骰也恢复了一半。", List.of());
    }

    /**
     * 短休：消耗生命骰恢复部分 HP，邪术师恢复法术位
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> shortRest(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        StringBuilder msg = new StringBuilder();
        msg.append("你进行了一次短休。");

        // 消耗生命骰恢复 HP（玩家可选择消耗多少，这里简化为消耗1个）
        if (state.getHitDice() > 0) {
            int healAmount = diceTool.rollDice(1, state.getHitDie()) + state.getConMod();
            int actualHeal = hpTool.applyHeal(state, healAmount);
            state.setHitDice(state.getHitDice() - 1);
            msg.append("消耗1个生命骰(d").append(state.getHitDie()).append(")，恢复")
               .append(actualHeal).append("点HP。");
        } else {
            msg.append("生命骰已耗尽，仅可喘息。");
        }

        // 邪术师短休恢复所有法术位
        String className = state.getClassName();
        if (className != null && (className.contains("邪术") || className.toLowerCase().contains("warlock"))) {
            state.setSpellSlots(new HashMap<>(state.getMaxSpellSlots()));
            msg.append("邪术师之力涌回，法术位已恢复。");
        }

        state.setPhase("EXPLORE");
        saveState(state);

        return buildDonePayload(state, msg.toString(), List.of());
    }

    /**
     * 使用背包物品（药水等消耗品）
     * 使用 ItemEffectProcessor 通用效果处理器，根据 magic_item.details JSON 处理效果。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> useItem(String sessionId, String itemId) {
        return useItemWithProcessor(sessionId, itemId, itemEffectProcessor);
    }

    /**
     * 使用物品（通过外部传入的 ItemEffectProcessor 处理效果）
     * 供 InventoryController 调用，统一物品效果处理入口。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> useItemWithProcessor(String sessionId, String itemId, ItemEffectProcessor processor) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        // 从背包查找物品
        AdventureState.InventoryItem target = null;
        for (AdventureState.InventoryItem item : state.getInventory()) {
            if (item.getItemId().equals(itemId) && item.getQuantity() > 0) {
                target = item;
                break;
            }
        }

        if (target == null) {
            return Map.of("error", "背包中没有此物品");
        }

        // 读取 magic_item.details JSON
        String detailsJson = target.getDetails();
        if (detailsJson == null || detailsJson.isBlank()) {
            // 尝试从数据库查询 magic_item
            try {
                MagicItem mi = magicItemService.getById(itemId);
                if (mi != null) {
                    detailsJson = mi.getDetails();
                    // 同步更新背包中的物品信息
                    target.setDetails(detailsJson);
                    target.setRarity(mi.getRarity());
                    target.setItemType(mi.getItemType());
                    if (target.getItemName() == null || target.getItemName().isBlank()) {
                        target.setItemName(mi.getName());
                    }
                }
            } catch (Exception ignored) {}
        }

        // 检查战斗中行动点（战斗中使用物品消耗行动点）
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null) {
            int actionCost = 1;
            // 从 details JSON 读取行动消耗
            if (detailsJson != null && !detailsJson.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode detailsNode = mapper.readTree(detailsJson);
                    if (detailsNode.has("actionCost")) {
                        actionCost = detailsNode.get("actionCost").asInt();
                    }
                } catch (Exception ignored) {}
            }
            if (state.getCombat().getActionPoints() < actionCost) {
                return Map.of("error", "行动点不足，无法使用物品（需要 " + actionCost + " 点行动点）");
            }
            // 消耗行动点
            state.getCombat().setActionPoints(state.getCombat().getActionPoints() - actionCost);
        }

        // 通过 ItemEffectProcessor 处理效果
        ItemEffectProcessor.EffectResult effectResult = processor.process(state, detailsJson);

        StringBuilder msg = new StringBuilder();
        if (effectResult.isSuccess()) {
            msg.append("你使用了").append(target.getItemName()).append("，").append(effectResult.getMessage());
        } else {
            msg.append("你使用了").append(target.getItemName()).append("，但").append(effectResult.getMessage());
        }

        // 减少数量
        target.setQuantity(target.getQuantity() - 1);
        if (target.getQuantity() <= 0) {
            state.getInventory().remove(target);
        }

        // 同步到数据库
        updateInventoryInDb(state.getCharacterId(), target.getItemId(), target.getQuantity());

        saveState(state);

        Map<String, Object> payload = buildDonePayload(state, msg.toString(), List.of());
        // 额外返回效果信息
        payload.put("effectType", effectResult.getEffectType());
        payload.put("actionPointsUsed", effectResult.getActionCost());
        if (state.getCombat() != null) {
            payload.put("remainingActionPoints", state.getCombat().getActionPoints());
        }
        return payload;
    }

    /**
     * 升级：处理玩家选择，更新角色数据
     * <p>
     * choices 结构:
     * {
     *   "asi": {"type": "stat", "stat": "str", "amount": 2} | {"type": "feat", "featId": "xxx"},
     *   "spellId": "spell_xxx",
     *   "combatStyle": "defense",
     *   "featId": "feat_xxx"
     * }
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> applyLevelUp(String sessionId, Map<String, Object> choices) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");
        if (!"LEVELUP".equals(state.getPhase())) return Map.of("error", "当前不可升级");

        int newLevel = state.getLevel() + 1;

        // 1. 处理升级奖励 (更新 PlayerCharacter + AdventureState 属性)
        List<String> acquired = levelUpService.processLevelUpRewards(state, choices);

        // 2. 更新等级
        state.setLevel(newLevel);

        // 3. 更新 HP（按新等级重算，保留当前损失比例）
        int oldMaxHp = state.getMaxHp();
        int hpLost = oldMaxHp - state.getCurrentHp();
        int hitDie = getHitDie(state.getClassName());
        int conMod = Math.max(0, state.getConMod()); // 使用可能被 ASI 更新后的 CON
        int avgHp = hitDie / 2 + 1 + conMod + 5;
        int newMaxHp = oldMaxHp + avgHp;
        state.setMaxHp(newMaxHp);
        state.setCurrentHp(Math.max(1, newMaxHp - hpLost));

        // 4. 更新生命骰上限
        state.setMaxHitDice(newLevel);
        state.setHitDice(state.getHitDice() + 1);

        // 5. 更新法术位
        Map<Integer, Integer> newSlots = getSpellSlotsForLevel(newLevel);
        state.setMaxSpellSlots(new HashMap<>(newSlots));
        state.setSpellSlots(new HashMap<>(newSlots));

        // 6. 将获得的奖励添加到 spells 列表 (供 AI 叙事使用)
        for (String desc : acquired) {
            state.getSpells().add(desc);
        }

        // 7. 更新 XP 阈值，清除升级状态
        state.setXpToNext(getXpToNext(newLevel));
        state.setLevelUpChoices(null);
        state.setPhase("EXPLORE");
        saveState(state);

        // 8. 构建返回数据
        Map<String, String> lvParams = new LinkedHashMap<>();
        lvParams.put("level", String.valueOf(newLevel));
        lvParams.put("className", state.getClassName());
        lvParams.put("maxHp", String.valueOf(newMaxHp));
        String narrative = formatPrompt("levelup_complete", lvParams);

        // 将获得的奖励附加到叙事中
        if (!acquired.isEmpty()) {
            StringBuilder sb = new StringBuilder(narrative);
            sb.append("\n\n**升级获得：**\n");
            for (String desc : acquired) {
                sb.append("- ").append(desc).append("\n");
            }
            narrative = sb.toString();
        }

        return buildDonePayload(state, narrative, List.of("继续冒险"));
    }


    // ════════════════════════════════════════════════════════════
    //  探索阶段
    // ════════════════════════════════════════════════════════════

    /**
     * 确认遭遇并流式发送战斗开始叙事
     */
    private void confirmEncounterAndStream(AdventureState state, SseEmitter emitter) {
        state.setPhase("COMBAT");
        state.setEncounterInfo(null);
        if (state.getCombat() != null) {
            state.getCombat().setCombatPhase("PLAYER_TURN");
            state.getCombat().setActionPoints(state.getCombat().getMaxActionPoints());
        }
        saveState(state);
        streamAgentResponse(state, "战斗开始，玩家进入回合制战斗。请叙述战斗场景并给出战斗选项。", emitter);
    }

    private void processExploreAction(AdventureState state, String action, SseEmitter emitter) {
        state.setExploreTurnCount(state.getExploreTurnCount() + 1);

        // 每 2-3 回合触发战斗
        boolean triggerCombat = state.getExploreTurnCount() >= 3
                && ThreadLocalRandom.current().nextDouble() < 0.7;

        if (triggerCombat) {
            triggerCombatEncounter(state, emitter);
        } else {
            streamNarrative(state, action, emitter);
        }
    }

    /**
     * 触发战斗遭遇：通过 EncounterTool 从怪物数据库按等级选怪
     * 进入 ENCOUNTER 状态，前端显示遭遇弹窗，等待玩家确认后进入 COMBAT。
     */
    private void triggerCombatEncounter(AdventureState state, SseEmitter emitter) {
        // 所有怪物数据来自数据库，属性由 EncounterTool 从 monster 表读取
        List<AdventureState.Enemy> enemies = encounterTool.generateEncounter(state.getLevel(), state.getLocation());

        // 创建战斗状态（但不立即进入 COMBAT，先进入 ENCOUNTER）
        AdventureState.CombatState combat = new AdventureState.CombatState();
        combat.setRound(1);
        combat.setEnemies(enemies);
        state.setCombat(combat);
        state.setPhase("ENCOUNTER");
        state.setExploreTurnCount(0);

        // 构建遭遇信息（供前端展示遭遇弹窗）
        AdventureState.EncounterInfo encounterInfo = new AdventureState.EncounterInfo();
        encounterInfo.setEnemyCount(enemies.size());
        encounterInfo.setLocation(state.getLocation());
        encounterInfo.setDangerLevel(calculateDangerLevel(enemies, state.getLevel()));

        // 构建敌人预览列表
        for (AdventureState.Enemy e : enemies) {
            AdventureState.EncounterInfo.EnemyPreview preview = new AdventureState.EncounterInfo.EnemyPreview(
                    e.getName(), e.getHp(), e.getAc(), estimateCrFromXp(e.getXpReward()), e.getXpReward());
            preview.setMonsterId(e.getMonsterId());
            preview.setImageUrl(e.getImageUrl());
            preview.setDamageType(e.getDamageType());
            encounterInfo.getEnemies().add(preview);
        }
        state.setEncounterInfo(encounterInfo);

        saveState(state);

        // 让 AI 叙述遭遇场景
        String enemyDesc = buildEnemyDescription(enemies);
        Map<String, String> ctParams = new HashMap<>();
        ctParams.put("enemyDesc", enemyDesc);
        streamNarrative(state, formatPrompt("combat_trigger", ctParams), emitter);
    }

    /**
     * 确认遭遇：玩家在遭遇弹窗点击"进入战斗"后调用，将 ENCOUNTER → COMBAT
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirmEncounter(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        if (!"ENCOUNTER".equals(state.getPhase())) {
            return Map.of("error", "当前不在遭遇状态");
        }

        // 切换到战斗状态
        state.setPhase("COMBAT");
        state.setEncounterInfo(null); // 清除遭遇信息
        // 确保战斗状态已初始化
        if (state.getCombat() == null) {
            return Map.of("error", "战斗状态异常，请重新开始冒险");
        }
        // 重置战斗子状态为玩家回合
        state.getCombat().setCombatPhase("PLAYER_TURN");
        state.getCombat().setActionPoints(state.getCombat().getMaxActionPoints());
        saveState(state);

        Map<String, Object> payload = buildDonePayload(state, "战斗开始！你进入了回合制战斗。", List.of());
        return payload;
    }

    /**
     * 获取角色面板数据（战斗中可调用，不退出战斗）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCharacterPanel(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        Map<String, Object> panel = new LinkedHashMap<>();

        // 基础信息
        panel.put("name", state.getCharacterName());
        panel.put("race", state.getRaceName());
        panel.put("class", state.getClassName());
        panel.put("level", state.getLevel());

        // 六维属性
        Map<String, Object> abilities = new LinkedHashMap<>();
        abilities.put("str", state.getStrength());
        abilities.put("dex", state.getDexterity());
        abilities.put("con", state.getConstitution());
        abilities.put("int", state.getIntelligence());
        abilities.put("wis", state.getWisdom());
        abilities.put("cha", state.getCharisma());
        abilities.put("strMod", state.getStrMod());
        abilities.put("dexMod", state.getDexMod());
        abilities.put("conMod", state.getConMod());
        abilities.put("intMod", state.getIntMod());
        abilities.put("wisMod", state.getWisMod());
        abilities.put("chaMod", state.getChaMod());
        panel.put("abilities", abilities);

        // 战斗属性
        Map<String, Object> combatStats = new LinkedHashMap<>();
        combatStats.put("hp", state.getCurrentHp());
        combatStats.put("maxHp", state.getMaxHp());
        combatStats.put("ac", state.getAc());
        combatStats.put("speed", 30); // 默认速度
        combatStats.put("xp", state.getXp());
        combatStats.put("xpToNext", state.getXpToNext());
        combatStats.put("hitDie", state.getHitDie());
        combatStats.put("hitDice", state.getHitDice());
        combatStats.put("maxHitDice", state.getMaxHitDice());
        panel.put("combatStats", combatStats);

        // 法术位
        panel.put("spellSlots", state.getSpellSlots());
        panel.put("maxSpellSlots", state.getMaxSpellSlots());

        // 法术列表
        panel.put("spells", state.getSpells());

        // 武器信息 — 从 character_inventory 读取已装备武器
        try {
            List<CharacterInventory> equippedWeapon = inventoryService.list(new LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, state.getCharacterId())
                    .eq(CharacterInventory::getIsEquipped, true)
                    .eq(CharacterInventory::getSlot, "WEAPON"));
            if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
                MagicItem weaponItem = magicItemService.getById(equippedWeapon.get(0).getItemId());
                if (weaponItem != null) {
                    Map<String, Object> weaponMap = new LinkedHashMap<>();
                    weaponMap.put("name", weaponItem.getName());
                    if (weaponItem.getDetails() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> details = mapper.readValue(weaponItem.getDetails(), Map.class);
                        weaponMap.put("damage", details.getOrDefault("damageDice", "1d6"));
                        weaponMap.put("damageType", details.getOrDefault("damageType", "物理"));
                        weaponMap.put("attackBonus", details.getOrDefault("attackBonus", 0));
                    }
                    panel.put("weapon", weaponMap);
                } else {
                    panel.put("weapon", null);
                }
            } else {
                panel.put("weapon", null);
            }
        } catch (Exception e) {
            panel.put("weapon", null);
        }

        // 护甲信息 — 从 character_inventory 读取已装备护甲
        try {
            List<CharacterInventory> equippedArmor = inventoryService.list(new LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, state.getCharacterId())
                    .eq(CharacterInventory::getIsEquipped, true)
                    .eq(CharacterInventory::getSlot, "ARMOR"));
            if (equippedArmor != null && !equippedArmor.isEmpty()) {
                MagicItem armorItem = magicItemService.getById(equippedArmor.get(0).getItemId());
                if (armorItem != null) {
                    Map<String, Object> armorMap = new LinkedHashMap<>();
                    armorMap.put("name", armorItem.getName());
                    if (armorItem.getDetails() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> details = mapper.readValue(armorItem.getDetails(), Map.class);
                        armorMap.put("acBonus", details.getOrDefault("acBonus", 0));
                    }
                    panel.put("armor", armorMap);
                } else {
                    panel.put("armor", null);
                }
            } else {
                panel.put("armor", null);
            }
        } catch (Exception e) {
            panel.put("armor", null);
        }

        // 当前状态（战斗中时）
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null) {
            Map<String, Object> combatInfo = new LinkedHashMap<>();
            combatInfo.put("round", state.getCombat().getRound());
            combatInfo.put("actionPoints", state.getCombat().getActionPoints());
            combatInfo.put("maxActionPoints", state.getCombat().getMaxActionPoints());
            combatInfo.put("combatPhase", state.getCombat().getCombatPhase());
            panel.put("combatInfo", combatInfo);
        }

        return panel;
    }

    /**
     * 结束回合：玩家主动结束当前回合，触发敌人回合
     * 每个存活敌人依次攻击，每个攻击结果作为单独条目返回（前端逐条显示）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> endTurn(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");

        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return Map.of("error", "当前不在战斗中");
        }

        // 安全网：如果所有敌人已死但胜利未结算（如物品击杀遗漏），先处理胜利
        ensureVictoryProcessed(state);
        if (state.getCombatResult() != null) {
            saveState(state);
            Map<String, Object> payload = buildDonePayload(state,
                    "所有敌人已被击败，战斗胜利！", List.of());
            state.setCombatResult(null);
            saveState(state);
            return payload;
        }

        // 设置为敌人回合
        state.getCombat().setCombatPhase("ENEMY_TURN");

        // 逐个处理每个存活敌人，生成独立的战斗日志条目
        List<String> enemyAttackEntries = new ArrayList<>();
        StringBuilder enemySummary = new StringBuilder();
        List<AdventureState.Enemy> aliveEnemies = state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive).toList();

        for (AdventureState.Enemy enemy : aliveEnemies) {
            AttackTool.AttackResult atkResult = attackTool.enemyAttack(enemy, state.getAc());

            StringBuilder enemyEntry = new StringBuilder();
            enemyEntry.append(String.format("【%s回合】", enemy.getName()));

            if (atkResult.hit) {
                int damage = diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice()) + enemy.getDamageBonus();
                if (atkResult.crit) damage += diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice());
                hpTool.applyDamageToPlayer(state, damage);
                enemyEntry.append(String.format("命中你%s，造成%d点%s伤害。当前HP: %d/%d。",
                        atkResult.crit ? "（暴击！）" : "", damage,
                        enemy.getDamageType() != null ? enemy.getDamageType() : "物理",
                        state.getCurrentHp(), state.getMaxHp()));
            } else {
                enemyEntry.append(String.format("的攻击落空了（掷骰: %d vs AC %d）。",
                        atkResult.roll, atkResult.targetAc));
            }

            String entryText = enemyEntry.toString();
            enemyAttackEntries.add(entryText);
            enemySummary.append(entryText).append(" ");

            // 保存中间状态，确保每个敌人的攻击后状态都被持久化
            saveState(state);

            // 检查玩家是否在当前敌人攻击后死亡
            if (hpTool.isDead(state.getCurrentHp())) {
                state.setCurrentHp(0);
                state.setPhase("DEAD");
                state.setCombat(null);
                saveState(state);
                Map<String, Object> payload = buildDonePayload(state,
                        enemySummary.toString().trim() + "\n\n你的生命值归零，倒在了血泊中...冒险到此结束。", List.of());
                // 返回逐条日志供前端展示
                payload.put("enemyAttackEntries", enemyAttackEntries);
                // 删除存档
                redisTemplate.delete(REDIS_KEY_PREFIX + state.getSessionId());
                return payload;
            }
        }

        // 所有敌人行动完毕：回合数+1，重置行动点，回到玩家回合
        state.getCombat().setRound(state.getCombat().getRound() + 1);
        state.getCombat().setActionPoints(state.getCombat().getMaxActionPoints());
        state.getCombat().setCombatPhase("PLAYER_TURN");
        saveState(state);

        Map<String, Object> payload = buildDonePayload(state, enemySummary.toString().trim(), List.of());
        // 返回逐条日志供前端展示
        payload.put("enemyAttackEntries", enemyAttackEntries);
        return payload;
    }

    /**
     * 根据敌人 XP 奖励推算 CR 字符串
     */
    private String estimateCrFromXp(int xpReward) {
        if (xpReward <= 40) return "1/8";
        if (xpReward <= 80) return "1/4";
        if (xpReward <= 150) return "1/2";
        if (xpReward <= 300) return "1";
        if (xpReward <= 450) return "2";
        if (xpReward <= 700) return "3";
        if (xpReward <= 1100) return "4";
        if (xpReward <= 1800) return "5";
        return String.valueOf(xpReward / 350);
    }

    /**
     * 计算危险等级
     */
    private String calculateDangerLevel(List<AdventureState.Enemy> enemies, int playerLevel) {
        int totalXp = enemies.stream().mapToInt(AdventureState.Enemy::getXpReward).sum();
        int threshold = playerLevel * 300; // 简化阈值
        if (totalXp < threshold * 0.3) return "TRIVIAL";
        if (totalXp < threshold * 0.6) return "EASY";
        if (totalXp < threshold * 1.0) return "MEDIUM";
        if (totalXp < threshold * 1.5) return "HARD";
        return "DEADLY";
    }

    // ════════════════════════════════════════════════════════════
    //  战斗阶段
    // ════════════════════════════════════════════════════════════

    private void processCombatAction(AdventureState state, String action, SseEmitter emitter) {
        // 1. 解析并执行玩家行动
        CombatResolution resolution = resolvePlayerCombatAction(state, action);

        // 2. 检查是否所有敌人已死
        boolean allDead = state.getCombat().getEnemies().stream().noneMatch(AdventureState.Enemy::isAlive);

        if (allDead) {
            // 战斗胜利
            int totalXp = state.getCombat().getEnemies().stream()
                    .mapToInt(AdventureState.Enemy::getXpReward).sum();
            state.setXp(state.getXp() + totalXp);

            // 生成战利品（通过 LootTool 从数据库物品表生成）
            List<AdventureState.LootItem> loot = lootTool.generateLoot(state.getCombat().getEnemies());

            // 将战利品写入背包
            addLootToInventory(state, loot);

            // 构建战斗结算结果
            AdventureState.CombatResult cr = new AdventureState.CombatResult();
            cr.setXpGained(totalXp);
            cr.setLoot(loot);
            state.setCombatResult(cr);

            state.setCombat(null);

            // 检查升级
            if (state.getXp() >= state.getXpToNext() && state.getLevel() < XP_THRESHOLDS.length) {
                state.setPhase("LEVELUP");
                state.setLevelUpChoices(buildLevelUpChoices(state));
                cr.setLeveledUp(true);
                cr.setNewLevel(state.getLevel() + 1);
                saveState(state);

                // AI 叙述胜利 + 升级提示
                Map<String, String> vlParams = new HashMap<>();
                vlParams.put("summary", resolution.summary);
                vlParams.put("xp", String.valueOf(totalXp));
                vlParams.put("newLevel", String.valueOf(state.getLevel() + 1));
                streamNarrative(state, formatPrompt("victory_levelup", vlParams), emitter);
            } else {
                state.setPhase("EXPLORE");
                saveState(state);

                Map<String, String> veParams = new HashMap<>();
                veParams.put("summary", resolution.summary);
                veParams.put("xp", String.valueOf(totalXp));
                veParams.put("currentHp", String.valueOf(state.getCurrentHp()));
                veParams.put("maxHp", String.valueOf(state.getMaxHp()));
                streamNarrative(state, formatPrompt("victory_explore", veParams), emitter);
            }
            return;
        }

        // 3. 检查逃跑
        if (resolution.fled) {
            state.setCombat(null);
            state.setPhase("EXPLORE");
            saveState(state);

            Map<String, String> fledParams = new HashMap<>();
            fledParams.put("summary", resolution.summary);
            streamNarrative(state, formatPrompt("fled", fledParams), emitter);
            return;
        }

        // 4. 检查玩家死亡（HpTool）
        if (hpTool.isDead(state.getCurrentHp())) {
            state.setCurrentHp(0);
            state.setPhase("DEAD");
            state.setCombat(null);
            saveState(state);

            // 发送死亡事件
            try {
                emitter.send(SseEmitter.event().name("token").data(resolution.summary));
                emitter.send(SseEmitter.event().name("token").data("\n\n你的生命值归零，倒在了血泊中...冒险到此结束。"));
                Thread.sleep(500);
                Map<String, Object> done = buildDonePayload(state,
                        resolution.summary + "\n\n你的生命值归零，倒在了血泊中...冒险到此结束。",
                        List.of());
                emitter.send(SseEmitter.event().name("done").data(mapper.writeValueAsString(done)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }

            // 删除存档
            redisTemplate.delete(REDIS_KEY_PREFIX + state.getSessionId());
            return;
        }

        // 5. 敌人回合
        String enemyTurnSummary = resolveEnemyTurn(state);

        // 6. 再次检查玩家死亡（敌人攻击后，HpTool）
        if (hpTool.isDead(state.getCurrentHp())) {
            state.setCurrentHp(0);
            state.setPhase("DEAD");
            state.setCombat(null);
            saveState(state);

            try {
                String fullSummary = resolution.summary + "\n" + enemyTurnSummary
                        + "\n\n敌人的致命一击让你倒下...生命值归零，冒险到此结束。";
                emitter.send(SseEmitter.event().name("token").data(fullSummary));
                Thread.sleep(500);
                Map<String, Object> done = buildDonePayload(state, fullSummary, List.of());
                emitter.send(SseEmitter.event().name("done").data(mapper.writeValueAsString(done)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }

            redisTemplate.delete(REDIS_KEY_PREFIX + state.getSessionId());
            return;
        }

        // 7. 更新回合，让 AI 叙述战斗结果
        state.getCombat().setRound(state.getCombat().getRound() + 1);
        // 重置行动点（新回合开始）
        state.getCombat().setActionPoints(state.getCombat().getMaxActionPoints());
        state.getCombat().setLastSummary(resolution.summary + "\n" + enemyTurnSummary);
        saveState(state);

        Map<String, String> crParams = new HashMap<>();
        crParams.put("round", String.valueOf(state.getCombat().getRound() - 1));
        crParams.put("summary", resolution.summary);
        crParams.put("enemyTurn", enemyTurnSummary);
        crParams.put("currentHp", String.valueOf(state.getCurrentHp()));
        crParams.put("maxHp", String.valueOf(state.getMaxHp()));
        crParams.put("enemyStatus", buildEnemyStatus(state));
        streamNarrative(state, formatPrompt("combat_round", crParams), emitter);
    }

    /**
     * 解析玩家战斗行动并执行（所有数值计算通过 Tool 完成）
     */
    private CombatResolution resolvePlayerCombatAction(AdventureState state, String action) {
        // 检测使用药水/消耗品
        if (action.contains("药水") || action.contains("使用") || action.contains("喝")) {
            // 尝试从背包匹配药水
            for (AdventureState.InventoryItem item : state.getInventory()) {
                if ("consumable".equals(item.getItemType()) && item.getQuantity() > 0
                        && action.contains(item.getItemName())) {
                    return resolveUseItem(state, item);
                }
            }
            // 如果提到"药水"但没指定，用第一瓶治疗药水
            if (action.contains("药水")) {
                for (AdventureState.InventoryItem item : state.getInventory()) {
                    if ("consumable".equals(item.getItemType()) && item.getQuantity() > 0
                            && item.getItemName().contains("治疗")) {
                        return resolveUseItem(state, item);
                    }
                }
            }
        }

        // 检测施法：通过 SpellTool 验证角色是否拥有该法术
        SpellTool.SpellValidation spellVal = spellTool.validateSpell(state, action);
        if (spellVal.success) {
            return resolveSpellCast(state, spellVal.spell, spellVal.slotLevelUsed, action);
        }

        // 如果匹配到法术名但验证失败，返回失败原因
        if (spellVal.spell != null && !spellVal.success) {
            CombatResolution res = new CombatResolution();
            res.summary = spellVal.reason + "\n";
            return res;
        }

        // 逃跑
        if (action.contains("逃") || action.contains("撤退") || action.contains("脱离")) {
            return resolveFlee(state);
        }

        // 检测远程攻击：关键词或武器类型
        boolean isRanged = action.contains("射") || action.contains("射击") || action.contains("弓")
                || action.contains("弩") || action.contains("远程") || action.contains("投掷");
        Weapon weapon = getPlayerWeapon(state);
        if (weapon != null && weapon.getCategory() != null && weapon.getCategory().contains("远程")) {
            isRanged = true; // 武器是远程类型，默认远程攻击
        }

        if (isRanged) {
            return resolveRangedAttack(state, action);
        }

        // 近战攻击
        return resolveMeleeAttack(state, action);
    }

    /**
     * 施法（所有数值通过 Tool 计算）
     * - 法术验证：SpellTool
     * - 伤害计算：DamageTool（从 spell 表 details.damageDice 读取）
     * - 攻击检定：AttackTool
     * - HP变化：HpTool
     * - 多目标法术：SpellTool.isMultiTargetSpell 检测，同时打击所有活着的敌人
     */
    private CombatResolution resolveSpellCast(AdventureState state, Spell spell, int slotLevelUsed, String action) {
        CombatResolution res = new CombatResolution();
        StringBuilder sb = new StringBuilder();

        // 消耗法术位（戏法不需要）
        if (slotLevelUsed > 0) {
            spellTool.consumeSlot(state, slotLevelUsed);
        }

        // 从数据库读取法术伤害（DamageTool 直接解析 spell.details.damageDice）
        DamageTool.DamageResult dmgResult = damageTool.rollSpellDamage(spell);
        String spellName = spell.getName();
        boolean isHeal = spellTool.isHealSpell(spell);

        // ── 治疗法术：恢复玩家HP ──
        if (isHeal && dmgResult.damage > 0) {
            int actualHeal = hpTool.applyHeal(state, dmgResult.damage);
            int before = state.getCurrentHp() - actualHeal;
            sb.append("你施放").append(spellName);
            if (slotLevelUsed > 0) sb.append("（消耗").append(slotLevelUsed).append("环法术位）");
            sb.append("，恢复").append(actualHeal).append("点生命值（").append(before).append("→").append(state.getCurrentHp()).append("）。\n");
            res.summary = sb.toString();
            return res;
        }

        // ── 增益/非伤害法术 ──
        if (dmgResult.damage == 0) {
            sb.append("你施放").append(spellName);
            if (slotLevelUsed > 0) sb.append("（消耗").append(slotLevelUsed).append("环法术位）");
            sb.append("。").append(spell.getSummary() != null ? spell.getSummary() : "效果生效。").append("\n");
            res.summary = sb.toString();
            return res;
        }

        // ── 伤害法术 ──
        boolean autoHit = spellTool.isAutoHitSpell(spell);
        boolean isMultiTarget = spellTool.isMultiTargetSpell(spell);

        // 获取目标列表
        List<AdventureState.Enemy> aliveEnemies = state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive).toList();

        if (aliveEnemies.isEmpty()) {
            res.summary = "没有可攻击的目标。";
            return res;
        }

        // 多目标法术：对所有活着的敌人造成伤害
        if (isMultiTarget) {
            sb.append("你施放").append(spellName);
            if (slotLevelUsed > 0) sb.append("（消耗").append(slotLevelUsed).append("环法术位）");
            sb.append("，范围效果席卷战场！\n");

            for (AdventureState.Enemy target : aliveEnemies) {
                int damage = dmgResult.damage; // 多目标法术每个目标受到相同伤害
                // 多目标法术通常不需要攻击检定（豁免检定由DM简化处理）
                hpTool.applyDamageToEnemy(target, damage);
                sb.append("  → ").append(target.getName()).append("受到")
                  .append(damage).append("点").append(dmgResult.damageType).append("伤害");
                if (!target.isAlive()) sb.append("，应声倒下！");
                sb.append("\n");
            }
            res.summary = sb.toString();
            return res;
        }

        // 单目标伤害法术
        AdventureState.Enemy target = aliveEnemies.get(0);

        if (!autoHit) {
            // 法术攻击检定（AttackTool，使用施法属性调整值）
            AttackTool.AttackResult atkResult = attackTool.playerSpellAttack(state, target);
            if (!atkResult.hit) {
                sb.append("你施放").append(spellName).append("，但被").append(target.getName()).append("闪避了！\n");
                res.summary = sb.toString();
                return res;
            }
        }

        // 施加伤害（HpTool）
        int damage = dmgResult.damage;
        hpTool.applyDamageToEnemy(target, damage);

        sb.append("你施放").append(spellName);
        if (slotLevelUsed > 0) sb.append("（消耗").append(slotLevelUsed).append("环法术位）");
        sb.append("，");
        if (autoHit) sb.append("魔力自动命中");
        else sb.append("精准命中");
        sb.append(target.getName()).append("，造成").append(damage).append("点").append(dmgResult.damageType).append("伤害。");
        if (!target.isAlive()) sb.append(target.getName()).append("应声倒下！");
        sb.append("\n");

        res.summary = sb.toString();
        return res;
    }

    /**
     * 近战攻击（所有数值通过 Tool 计算）
     * - 武器伤害：从 weapon 数据库读取（通过角色 weapon_id）
     * - 攻击检定：AttackTool（熟练+STR_mod）
     * - 伤害计算：DamageTool（暴击时骰子翻倍 + STR_mod）
     * - HP变化：HpTool
     */
    private CombatResolution resolveMeleeAttack(AdventureState state, String action) {
        CombatResolution res = new CombatResolution();
        AdventureState.Enemy target = state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive).findFirst().orElse(null);
        if (target == null) {
            res.summary = "没有可攻击的目标。";
            return res;
        }

        StringBuilder sb = new StringBuilder();

        // 攻击检定（AttackTool，使用 STR_mod）
        AttackTool.AttackResult atkResult = attackTool.playerMeleeAttack(state, target);

        if (atkResult.hit) {
            // 从数据库获取玩家武器
            Weapon weapon = getPlayerWeapon(state);

            // 伤害计算（DamageTool，暴击时骰子翻倍，加 STR_mod）
            DamageTool.DamageResult dmgResult;
            if (weapon != null) {
                dmgResult = damageTool.rollWeaponDamage(weapon, atkResult.crit, state.getStrMod());
            } else {
                // 无武器时使用徒手 1d3 + STR_mod
                dmgResult = new DamageTool.DamageResult();
                int diceDmg = diceTool.rollDice(1, 3);
                if (atkResult.crit) diceDmg *= 2;
                dmgResult.damage = diceDmg + Math.max(0, state.getStrMod());
                dmgResult.damageType = "钝击";
                dmgResult.crit = atkResult.crit;
            }

            int damage = dmgResult.damage;

            // 施加伤害（HpTool）
            hpTool.applyDamageToEnemy(target, damage);

            sb.append("你的").append(weapon != null ? weapon.getName() : "徒手攻击");
            if (atkResult.crit) sb.append("暴击");
            sb.append("命中").append(target.getName());
            sb.append("，造成").append(damage).append("点").append(dmgResult.damageType).append("伤害。");
            if (!target.isAlive()) sb.append(target.getName()).append("倒地不起！");
        } else {
            sb.append("你挥出一击，但").append(target.getName()).append("格挡开了。");
        }
        sb.append("\n");

        res.summary = sb.toString();
        return res;
    }

    /**
     * 远程攻击（所有数值通过 Tool 计算）
     * - 武器伤害：从 weapon 数据库读取
     * - 攻击检定：AttackTool（熟练+DEX_mod）
     * - 伤害计算：DamageTool（暴击时骰子翻倍 + DEX_mod）
     * - HP变化：HpTool
     */
    private CombatResolution resolveRangedAttack(AdventureState state, String action) {
        CombatResolution res = new CombatResolution();
        AdventureState.Enemy target = state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive).findFirst().orElse(null);
        if (target == null) {
            res.summary = "没有可攻击的目标。";
            return res;
        }

        StringBuilder sb = new StringBuilder();

        // 攻击检定（AttackTool，使用 DEX_mod）
        AttackTool.AttackResult atkResult = attackTool.playerRangedAttack(state, target);

        if (atkResult.hit) {
            // 从数据库获取玩家武器
            Weapon weapon = getPlayerWeapon(state);

            // 伤害计算（DamageTool，暴击时骰子翻倍，加 DEX_mod）
            DamageTool.DamageResult dmgResult;
            if (weapon != null) {
                dmgResult = damageTool.rollWeaponDamage(weapon, atkResult.crit, state.getDexMod());
            } else {
                // 无远程武器时使用即兴投掷 1d4 + DEX_mod
                dmgResult = new DamageTool.DamageResult();
                int diceDmg = diceTool.rollDice(1, 4);
                if (atkResult.crit) diceDmg *= 2;
                dmgResult.damage = diceDmg + Math.max(0, state.getDexMod());
                dmgResult.damageType = "穿刺";
                dmgResult.crit = atkResult.crit;
            }

            int damage = dmgResult.damage;

            // 施加伤害（HpTool）
            hpTool.applyDamageToEnemy(target, damage);

            sb.append("你的").append(weapon != null ? weapon.getName() : "即兴投掷");
            if (atkResult.crit) sb.append("暴击");
            sb.append("命中").append(target.getName());
            sb.append("，造成").append(damage).append("点").append(dmgResult.damageType).append("伤害。");
            if (!target.isAlive()) sb.append(target.getName()).append("倒地不起！");
        } else {
            sb.append("你的攻击偏了，").append(target.getName()).append("闪身躲开。");
        }
        sb.append("\n");

        res.summary = sb.toString();
        return res;
    }

    /**
     * 使用消耗品（药水等）
     */
    private CombatResolution resolveUseItem(AdventureState state, AdventureState.InventoryItem item) {
        CombatResolution res = new CombatResolution();
        StringBuilder sb = new StringBuilder();

        // 治疗药水：恢复 2d4+2 HP
        if ("consumable".equals(item.getItemType()) && item.getItemName().contains("治疗")) {
            int healAmount = diceTool.rollDice(2, 4) + 2;
            int actualHeal = hpTool.applyHeal(state, healAmount);
            sb.append("你使用了一瓶").append(item.getItemName())
              .append("，恢复").append(actualHeal).append("点HP。\n");

            // 减少数量
            item.setQuantity(item.getQuantity() - 1);
            if (item.getQuantity() <= 0) {
                state.getInventory().remove(item);
            }

            // 同步到数据库
            updateInventoryInDb(state.getCharacterId(), item.getItemId(), item.getQuantity());
        } else {
            sb.append("你使用了").append(item.getItemName()).append("，但似乎没有效果。\n");
        }

        res.summary = sb.toString();
        return res;
    }

    /**
     * 逃跑（骰子通过 DiceTool）
     */
    private CombatResolution resolveFlee(AdventureState state) {
        CombatResolution res = new CombatResolution();
        int fleeDc = 10 + state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive)
                .mapToInt(e -> e.getXpReward())
                .max().orElse(50) / 50;
        int roll = diceTool.rollD20() + state.getDexMod(); // + DEX_mod
        if (roll >= fleeDc) {
            res.fled = true;
            res.summary = "你成功脱离了战斗！\n";
        } else {
            res.summary = "你试图逃跑，但被敌人拦住了去路，逃跑失败！\n";
        }
        return res;
    }

    /**
     * 敌人回合（所有数值通过 Tool 计算）
     * - 攻击检定：AttackTool（使用敌人从数据库读取的 attackBonus）
     * - 伤害计算：DiceTool（使用敌人从数据库读取的 damageDice/Count/Bonus）
     * - HP变化：HpTool
     */
    private String resolveEnemyTurn(AdventureState state) {
        StringBuilder sb = new StringBuilder();
        for (AdventureState.Enemy enemy : state.getCombat().getEnemies()) {
            if (!enemy.isAlive()) continue;

            // 敌人攻击检定（AttackTool，attackBonus 来自怪物数据库）
            AttackTool.AttackResult atkResult = attackTool.enemyAttack(enemy, state.getAc());

            if (atkResult.hit) {
                // 敌人伤害（骰子来自怪物数据库的 damageFormula，已解析到 Enemy 字段）
                int damage = diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice()) + enemy.getDamageBonus();
                if (atkResult.crit) damage += diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice()); // 暴击加骰

                // 施加伤害（HpTool）
                hpTool.applyDamageToPlayer(state, damage);
                sb.append(enemy.getName()).append("命中了你");
                if (atkResult.crit) sb.append("（暴击！）");
                sb.append("，造成").append(damage).append("点").append(enemy.getDamageType() != null ? enemy.getDamageType() : "物理").append("伤害。\n");
            } else {
                sb.append(enemy.getName()).append("的攻击落空了。\n");
            }
        }
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    //  升级
    // ════════════════════════════════════════════════════════════

    /**
     * 构建升级选择数据 — 委托给 LevelUpService
     */
    private AdventureState.LevelUpChoices buildLevelUpChoices(AdventureState state) {
        return levelUpService.buildLevelUpChoices(state);
    }

    /**
     * 获取升级信息 (供 GET 接口返回)
     */
    public Map<String, Object> getLevelUpInfo(String sessionId) {
        AdventureState state = loadState(sessionId);
        if (state == null) return Map.of("error", "会话不存在");
        if (!"LEVELUP".equals(state.getPhase())) return Map.of("error", "当前不可升级");
        return levelUpService.getLevelUpInfo(state);
    }

    // ════════════════════════════════════════════════════════════
    //  AI 流式调用
    // ════════════════════════════════════════════════════════════

    private void streamNarrative(AdventureState state, String action, SseEmitter emitter) {
        List<ChatMessage> messages = buildMessages(state, action);
        StringBuilder fullText = new StringBuilder();

        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    fullText.append(partialResponse);
                    emitter.send(SseEmitter.event().name("token").data(partialResponse));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    ParsedResult result = parseResult(fullText.toString());

                    state.getHistory().add(new AdventureState.HistoryEntry("user", action));
                    state.getHistory().add(new AdventureState.HistoryEntry("assistant", result.narrative));
                    trimHistory(state);
                    state.setCurrentChoices(result.choices);
                    if (result.location != null && !result.location.isBlank()) {
                        state.setLocation(result.location);
                    }
                    saveState(state);

                    // 安全网：确保所有敌人被击败时胜利结算被处理
                    ensureVictoryProcessed(state);
                    saveState(state);

                    String doneJson = mapper.writeValueAsString(buildDonePayload(state, result.narrative, result.choices));
                    emitter.send(SseEmitter.event().name("done").data(doneJson));
                    // 发送完毕后清除战斗结算结果，避免重复显示
                    if (state.getCombatResult() != null) {
                        state.setCombatResult(null);
                        saveState(state);
                    }
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                sendError(emitter, "AI 生成失败: " + error.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Agent 模式 — AI 自主调用 Tool 执行游戏机制
    // ════════════════════════════════════════════════════════════

    /**
     * 构建 Agent：为当前会话状态创建一个 DungeonMasterAgent 实例。
     * <p>
     * 每次调用 processAction 时都会新建一个 Agent，
     * 因为 GameTools 需要绑定当前的 AdventureState。
     * <p>
     * Agent 内部通过 Function Calling 自主决定调用哪些 @Tool 方法，
     * 工具执行后返回结果给 AI，AI 基于结果生成叙事。
     *
     * @param state 当前冒险状态（GameTools 会直接修改此对象）
     * @return 配置好的 DungeonMasterAgent
     */
    private DungeonMasterAgent buildAgent(AdventureState state) {
        // 为当前状态创建 GameTools（每个 @Tool 方法操作同一个 state 实例）
        GameTools gameTools = new GameTools(
                state,
                diceTool,
                hpTool,
                attackTool,
                damageTool,
                spellTool,
                encounterTool,
                lootTool,
                abilityTool,
                itemEffectProcessor,
                weaponService,
                inventoryService,
                magicItemService,
                characterService,
                spellService
        );

        // 构建系统 Prompt（含角色信息、状态、战斗信息等）
        String systemPrompt = buildAgentSystemPrompt(state);

        // 使用 ChatMemory 维护对话历史，让 AI 有上下文
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        for (AdventureState.HistoryEntry entry : state.getHistory()) {
            if ("user".equals(entry.getRole())) {
                memory.add(UserMessage.from(entry.getContent()));
            } else if ("assistant".equals(entry.getRole())) {
                memory.add(AiMessage.from(entry.getContent()));
            }
        }

        return AiServices.builder(DungeonMasterAgent.class)
                .streamingChatModel(streamingModel)
                .systemMessageProvider(memoryId -> systemPrompt)
                .chatMemory(memory)
                .tools(gameTools)
                .maxSequentialToolsInvocations(10)
                .build();
    }

    /**
     * 构建 Agent 系统提示词（基于 system_agent.txt 模板）。
     * <p>
     * 与旧版 buildSystemPrompt 不同，此版本包含工具使用指南，
     * 指导 AI 通过 Function Calling 自主调用游戏工具。
     */
    private String buildAgentSystemPrompt(AdventureState state) {
        String spellList = state.getSpells().isEmpty()
                ? "无"
                : String.join("、", state.getSpells());

        // 战斗状态信息
        String combatInfo = "";
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null) {
            String phaseLabel = "ENEMY_TURN".equals(state.getCombat().getCombatPhase())
                    ? "敌人回合（等待玩家确认后执行）" : "玩家回合（等待玩家行动）";
            combatInfo = "\n【战斗状态】回合: " + state.getCombat().getRound()
                    + " | 阶段: " + phaseLabel + "\n" + buildEnemyStatus(state)
                    + "玩家HP: " + state.getCurrentHp() + "/" + state.getMaxHp() + "\n";
        }

        // 背包物品列表
        String inventoryList = "无";
        if (state.getInventory() != null && !state.getInventory().isEmpty()) {
            List<String> invParts = new ArrayList<>();
            for (AdventureState.InventoryItem item : state.getInventory()) {
                if (item.getQuantity() > 0) {
                    String equipped = item.isEquipped() ? " [已装备]" : "";
                    invParts.add(item.getItemName() + " x" + item.getQuantity() + equipped);
                }
            }
            inventoryList = invParts.isEmpty() ? "无" : String.join("、", invParts);
        }

        // 六维属性
        String abilityInfo = abilityTool.formatAbilityScores(state);
        String spellAbility = abilityTool.getSpellcastingAbilityLabel(state);

        // 武器信息
        String weaponInfo = "无武器";
        Weapon weapon = getPlayerWeapon(state);
        if (weapon != null) {
            String weaponType = (weapon.getCategory() != null && weapon.getCategory().contains("远程"))
                    ? "远程武器" : "近战武器";
            weaponInfo = weapon.getName() + "（" + weaponType + "，伤害" + weapon.getDamage() + "）";
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("characterName", state.getCharacterName());
        params.put("raceName", state.getRaceName());
        params.put("className", state.getClassName());
        params.put("level", String.valueOf(state.getLevel()));
        params.put("currentHp", String.valueOf(state.getCurrentHp()));
        params.put("maxHp", String.valueOf(state.getMaxHp()));
        params.put("ac", String.valueOf(state.getAc()));
        params.put("abilityScores", abilityInfo);
        params.put("spellAbility", spellAbility);
        params.put("weapon", weaponInfo);
        params.put("spellSlots", formatSpellSlots(state));
        params.put("xp", String.valueOf(state.getXp()));
        params.put("xpToNext", String.valueOf(state.getXpToNext()));
        params.put("location", state.getLocation() != null ? state.getLocation() : "未知之地");
        params.put("spellList", spellList);
        params.put("inventory", inventoryList);
        params.put("combatInfo", combatInfo);
        return formatPrompt("system_agent", params);
    }

    /**
     * 安全网：检测并处理未结算的战斗胜利。
     * <p>
     * 当所有敌人已被击败但 combatResult 未设置时（例如物品击杀未触发 autoCheckVictory，
     * 或 AI 叙述了胜利但未调用 checkVictory 工具），自动执行胜利结算，
     * 避免 combatResult 为 null 导致前端不显示胜利弹窗、界面卡在战斗页面。
     */
    private void ensureVictoryProcessed(AdventureState state) {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return;
        }
        if (state.getCombatResult() != null) {
            return; // 已处理
        }
        boolean allDead = state.getCombat().getEnemies().stream()
                .noneMatch(AdventureState.Enemy::isAlive);
        if (!allDead) {
            return;
        }

        log.warn("[Safety Net] 检测到未处理的战斗胜利，自动结算 (session={})", state.getSessionId());

        int totalXp = state.getCombat().getEnemies().stream()
                .mapToInt(AdventureState.Enemy::getXpReward).sum();
        state.setXp(state.getXp() + totalXp);

        List<AdventureState.LootItem> loot = lootTool.generateLoot(state.getCombat().getEnemies());
        addLootToInventory(state, loot);

        AdventureState.CombatResult cr = new AdventureState.CombatResult();
        cr.setXpGained(totalXp);
        cr.setLoot(loot);
        state.setCombatResult(cr);
        state.setCombat(null);

        // 检查升级
        if (state.getXp() >= state.getXpToNext() && state.getLevel() < XP_THRESHOLDS.length) {
            int newLevel = state.getLevel() + 1;
            state.setLevel(newLevel);
            state.setXpToNext(XP_THRESHOLDS[Math.min(newLevel, XP_THRESHOLDS.length - 1)]);

            int hpGain = state.getHitDie() / 2 + 1 + Math.max(0, state.getConMod()) + 5;
            state.setMaxHp(state.getMaxHp() + hpGain);
            state.setCurrentHp(state.getCurrentHp() + hpGain);

            if (newLevel <= SPELL_SLOT_TABLE.length) {
                int[] slots = SPELL_SLOT_TABLE[newLevel - 1];
                Map<Integer, Integer> newSlots = new HashMap<>();
                Map<Integer, Integer> newMaxSlots = new HashMap<>();
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] > 0) {
                        newSlots.put(i + 1, slots[i]);
                        newMaxSlots.put(i + 1, slots[i]);
                    }
                }
                state.setSpellSlots(newSlots);
                state.setMaxSpellSlots(newMaxSlots);
            }

            state.setPhase("LEVELUP");
            cr.setLeveledUp(true);
            cr.setNewLevel(newLevel);
        } else {
            state.setPhase("EXPLORE");
        }
    }

    /**
     * Agent 流式响应：通过 TokenStream 回调将 AI 输出推送到前端 SseEmitter。
     * <p>
     * 流程：
     * 1. AI 接收玩家行动 → 自主决定调用哪些 @Tool 方法
     * 2. 工具执行结果返回给 AI → AI 可能继续调用更多工具
     * 3. AI 基于所有工具结果生成叙事文本 → 逐 Token 流式推送
     * 4. 完成后保存状态、解析选项、发送 done 事件
     *
     * @param state  当前冒险状态（会被 GameTools 修改）
     * @param action 玩家行动描述（或开场提示）
     * @param emitter SSE 推送器
     */
    private void streamAgentResponse(AdventureState state, String action, SseEmitter emitter) {
        DungeonMasterAgent agent = buildAgent(state);
        TokenStream tokenStream = agent.chat(action);
        StringBuilder fullText = new StringBuilder();

        tokenStream
            .onPartialResponse(token -> {
                try {
                    fullText.append(token);
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            })
            .onToolExecuted(toolExecution -> {
                // 记录工具调用日志，便于调试和展示 Agent 决策过程
                String toolName = toolExecution.request().name();
                String toolResult = toolExecution.result();
                log.info("[Agent Tool] {} → {}", toolName, toolResult.length() > 200
                        ? toolResult.substring(0, 200) + "..." : toolResult);
            })
            .onCompleteResponse(response -> {
                try {
                    String narrative = fullText.toString();
                    ParsedResult result = parseResult(narrative);

                    // 更新对话历史
                    state.getHistory().add(new AdventureState.HistoryEntry("user", action));
                    state.getHistory().add(new AdventureState.HistoryEntry("assistant", result.narrative));
                    trimHistory(state);
                    state.setCurrentChoices(result.choices);
                    if (result.location != null && !result.location.isBlank()) {
                        state.setLocation(result.location);
                    }

                    // 保存状态到 Redis（GameTools 已在工具调用中修改了 state）
                    // 安全网：确保所有敌人被击败时胜利结算被处理（防止物品击杀等遗漏）
                    ensureVictoryProcessed(state);
                    saveState(state);

                    // 发送 done 事件
                    String doneJson = mapper.writeValueAsString(
                            buildDonePayload(state, result.narrative, result.choices));
                    emitter.send(SseEmitter.event().name("done").data(doneJson));

                    // 清除战斗结算结果，避免重复显示
                    if (state.getCombatResult() != null) {
                        state.setCombatResult(null);
                        saveState(state);
                    }
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Agent 完成回调异常", e);
                    emitter.completeWithError(e);
                }
            })
            .onError(error -> {
                log.error("Agent 执行失败", error);
                // 即使出错也尝试保存已修改的状态
                try {
                    saveState(state);
                } catch (Exception ignored) {}
                sendError(emitter, "Agent 执行失败: " + error.getMessage());
            })
            .start();
    }

    // ════════════════════════════════════════════════════════════
    //  Prompt 加载（从 classpath:prompts/*.txt 读取，带缓存）
    // ════════════════════════════════════════════════════════════

    /**
     * 从 classpath:prompts/{name}.txt 加载 prompt 模板，带缓存。
     * 文件只需在首次调用时读取一次。
     */
    private String loadPrompt(String name) {
        return promptCache.computeIfAbsent(name, n -> {
            try (InputStream is = new ClassPathResource("prompts/" + n + ".txt").getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                throw new RuntimeException("加载Prompt文件失败: prompts/" + n + ".txt", e);
            }
        });
    }

    /**
     * 加载 prompt 模板并替换 {key} 占位符。
     * 例如 txt 中 "{level}级" → params.put("level", "3") → "3级"
     */
    private String formatPrompt(String name, Map<String, String> params) {
        String template = loadPrompt(name);
        if (params == null || params.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════
    //  Prompt 构建
    // ════════════════════════════════════════════════════════════

    private List<ChatMessage> buildMessages(AdventureState state, String action) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildSystemPrompt(state)));
        for (AdventureState.HistoryEntry entry : state.getHistory()) {
            if ("user".equals(entry.getRole())) {
                messages.add(UserMessage.from(entry.getContent()));
            } else {
                messages.add(AiMessage.from(entry.getContent()));
            }
        }
        messages.add(UserMessage.from(action));
        return messages;
    }

    private String buildSystemPrompt(AdventureState state) {
        String spellList = state.getSpells().isEmpty()
                ? "无"
                : String.join("\n", state.getSpells());

        String combatInfo = "";
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null) {
            combatInfo = "\n【战斗状态】\n" + buildEnemyStatus(state)
                    + "玩家HP: " + state.getCurrentHp() + "/" + state.getMaxHp() + "\n";
        }

        // 背包物品列表
        String inventoryList = "无";
        if (state.getInventory() != null && !state.getInventory().isEmpty()) {
            List<String> invParts = new ArrayList<>();
            for (AdventureState.InventoryItem item : state.getInventory()) {
                if (item.getQuantity() > 0) {
                    String equipped = item.isEquipped() ? " [已装备]" : "";
                    invParts.add(item.getItemName() + " x" + item.getQuantity() + equipped);
                }
            }
            inventoryList = invParts.isEmpty() ? "无" : String.join("\n", invParts);
        }

        // 六维属性信息
        String abilityInfo = abilityTool.formatAbilityScores(state);
        String spellAbility = abilityTool.getSpellcastingAbilityLabel(state);

        // 玩家武器信息（区分近战/远程）
        String weaponInfo = "无武器";
        Weapon weapon = getPlayerWeapon(state);
        if (weapon != null) {
            String weaponType = (weapon.getCategory() != null && weapon.getCategory().contains("远程"))
                    ? "远程武器" : "近战武器";
            weaponInfo = weapon.getName() + "（" + weaponType + "，伤害" + weapon.getDamage() + "）";
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("characterName", state.getCharacterName());
        params.put("raceName", state.getRaceName());
        params.put("className", state.getClassName());
        params.put("level", String.valueOf(state.getLevel()));
        params.put("currentHp", String.valueOf(state.getCurrentHp()));
        params.put("maxHp", String.valueOf(state.getMaxHp()));
        params.put("ac", String.valueOf(state.getAc()));
        params.put("abilityScores", abilityInfo);
        params.put("spellAbility", spellAbility);
        params.put("weapon", weaponInfo);
        params.put("spellSlots", formatSpellSlots(state));
        params.put("xp", String.valueOf(state.getXp()));
        params.put("xpToNext", String.valueOf(state.getXpToNext()));
        params.put("location", state.getLocation());
        params.put("spellList", spellList);
        params.put("inventory", inventoryList);
        params.put("combatInfo", combatInfo);
        return formatPrompt("system", params);
    }

    // ════════════════════════════════════════════════════════════
    //  响应解析 & 构造 done 事件
    // ════════════════════════════════════════════════════════════

    private ParsedResult parseResult(String raw) {
        ParsedResult result = new ParsedResult();
        String[] parts = raw.split("---", 2);
        if (parts.length == 2) {
            result.narrative = parts[0].trim();
            String[] lines = parts[1].trim().split("\n");
            List<String> choices = new ArrayList<>();
            for (String line : lines) {
                String cleaned = line.replaceAll("^\\d+\\.\\s*", "").trim();
                if (!cleaned.isEmpty()) choices.add(cleaned);
            }
            result.choices = choices;
        } else {
            result.narrative = raw.trim();
            result.choices = List.of();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDonePayload(AdventureState state, String narrative, List<String> choices) {
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("narrative", narrative);
        done.put("choices", choices);
        done.put("phase", state.getPhase());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("hp", state.getCurrentHp());
        stats.put("maxHp", state.getMaxHp());
        stats.put("ac", state.getAc());
        stats.put("level", state.getLevel());
        stats.put("xp", state.getXp());
        stats.put("xpToNext", state.getXpToNext());
        stats.put("spellSlots", state.getSpellSlots());
        stats.put("maxSpellSlots", state.getMaxSpellSlots());

        // 六维属性
        Map<String, Object> abilities = new LinkedHashMap<>();
        abilities.put("str", state.getStrength());
        abilities.put("dex", state.getDexterity());
        abilities.put("con", state.getConstitution());
        abilities.put("int", state.getIntelligence());
        abilities.put("wis", state.getWisdom());
        abilities.put("cha", state.getCharisma());
        abilities.put("strMod", state.getStrMod());
        abilities.put("dexMod", state.getDexMod());
        abilities.put("conMod", state.getConMod());
        abilities.put("intMod", state.getIntMod());
        abilities.put("wisMod", state.getWisMod());
        abilities.put("chaMod", state.getChaMod());
        stats.put("abilities", abilities);

        // 生命骰
        stats.put("hitDie", state.getHitDie());
        stats.put("hitDice", state.getHitDice());
        stats.put("maxHitDice", state.getMaxHitDice());

        done.put("stats", stats);

        // 背包
        if (state.getInventory() != null && !state.getInventory().isEmpty()) {
            List<Map<String, Object>> invList = new ArrayList<>();
            for (AdventureState.InventoryItem item : state.getInventory()) {
                Map<String, Object> inv = new LinkedHashMap<>();
                inv.put("id", item.getId());
                inv.put("itemType", item.getItemType());
                inv.put("itemId", item.getItemId());
                inv.put("itemName", item.getItemName());
                inv.put("quantity", item.getQuantity());
                inv.put("equipped", item.isEquipped());
                inv.put("slot", item.getSlot());
                inv.put("rarity", item.getRarity());
                inv.put("details", item.getDetails());
                inv.put("summary", item.getSummary());
                // 解析效果类型供前端使用
                if (item.getDetails() != null && !item.getDetails().isBlank()) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode detailsNode = mapper.readTree(item.getDetails());
                        inv.put("effectType", detailsNode.has("effectType") ? detailsNode.get("effectType").asText() : null);
                        inv.put("actionCost", detailsNode.has("actionCost") ? detailsNode.get("actionCost").asInt() : 1);
                    } catch (Exception ignored) {
                        inv.put("effectType", null);
                        inv.put("actionCost", 1);
                    }
                } else {
                    inv.put("effectType", null);
                    inv.put("actionCost", 1);
                }
                invList.add(inv);
            }
            done.put("inventory", invList);
        } else {
            done.put("inventory", new ArrayList<>());
        }

        if (state.getCombat() != null) {
            Map<String, Object> combat = new LinkedHashMap<>();
            combat.put("round", state.getCombat().getRound());
            combat.put("lastSummary", state.getCombat().getLastSummary());
            combat.put("actionPoints", state.getCombat().getActionPoints());
            combat.put("maxActionPoints", state.getCombat().getMaxActionPoints());
            combat.put("combatPhase", state.getCombat().getCombatPhase());
            List<Map<String, Object>> enemies = new ArrayList<>();
            for (AdventureState.Enemy e : state.getCombat().getEnemies()) {
                Map<String, Object> enemy = new LinkedHashMap<>();
                enemy.put("name", e.getName());
                enemy.put("monsterId", e.getMonsterId());
                enemy.put("imageUrl", e.getImageUrl());
                enemy.put("hp", e.getHp());
                enemy.put("maxHp", e.getMaxHp());
                enemy.put("ac", e.getAc());
                enemy.put("alive", e.isAlive());
                enemy.put("xpReward", e.getXpReward());
                enemies.add(enemy);
            }
            combat.put("enemies", enemies);
            done.put("combat", combat);
        } else {
            done.put("combat", null);
        }

        // 法术列表（供前端法术选择弹窗使用）
        done.put("spells", state.getSpells() != null ? state.getSpells() : new ArrayList<>());

        if (state.getLevelUpChoices() != null) {
            done.put("levelUp", state.getLevelUpChoices());
        } else {
            done.put("levelUp", null);
        }

        // 战斗结算结果（仅战斗刚结束时存在）
        if (state.getCombatResult() != null) {
            AdventureState.CombatResult cr = state.getCombatResult();
            Map<String, Object> crMap = new LinkedHashMap<>();
            crMap.put("xpGained", cr.getXpGained());
            crMap.put("leveledUp", cr.isLeveledUp());
            crMap.put("newLevel", cr.getNewLevel());
            List<Map<String, Object>> lootList = new ArrayList<>();
            for (AdventureState.LootItem item : cr.getLoot()) {
                Map<String, Object> loot = new LinkedHashMap<>();
                loot.put("name", item.getName());
                loot.put("itemId", item.getItemId());
                loot.put("quantity", item.getQuantity());
                loot.put("icon", item.getIcon());
                loot.put("rarity", item.getRarity());
                loot.put("itemType", item.getItemType());
                lootList.add(loot);
            }
            crMap.put("loot", lootList);
            done.put("combatResult", crMap);
        } else {
            done.put("combatResult", null);
        }

        // 遭遇信息（仅 ENCOUNTER 阶段存在）
        if (state.getEncounterInfo() != null) {
            AdventureState.EncounterInfo ei = state.getEncounterInfo();
            Map<String, Object> eiMap = new LinkedHashMap<>();
            eiMap.put("enemyCount", ei.getEnemyCount());
            eiMap.put("dangerLevel", ei.getDangerLevel());
            eiMap.put("location", ei.getLocation());
            eiMap.put("description", ei.getDescription());
            List<Map<String, Object>> enemyPreviews = new ArrayList<>();
            for (AdventureState.EncounterInfo.EnemyPreview ep : ei.getEnemies()) {
                Map<String, Object> epMap = new LinkedHashMap<>();
                epMap.put("name", ep.getName());
                epMap.put("monsterId", ep.getMonsterId());
                epMap.put("imageUrl", ep.getImageUrl());
                epMap.put("hp", ep.getHp());
                epMap.put("maxHp", ep.getMaxHp());
                epMap.put("ac", ep.getAc());
                epMap.put("cr", ep.getCr());
                epMap.put("level", ep.getLevel());
                epMap.put("damageType", ep.getDamageType());
                epMap.put("xpReward", ep.getXpReward());
                enemyPreviews.add(epMap);
            }
            eiMap.put("enemies", enemyPreviews);
            done.put("encounterInfo", eiMap);
        } else {
            done.put("encounterInfo", null);
        }

        return done;
    }

    // ════════════════════════════════════════════════════════════
    //  初始化与工具方法
    // ════════════════════════════════════════════════════════════

    private void initStats(AdventureState state, int level, String className, PlayerCharacter pc) {
        int hitDie = getHitDie(className);
        int conMod = Math.max(0, state.getConMod()); // 体质调整值不低于0，防止负值减少HP

        // HP = hitDie + CON_mod + (level-1) * (hitDie/2 + 1 + CON_mod) + level * 5 (额外生命加成)
        int hp = hitDie + conMod + (level - 1) * (hitDie / 2 + 1 + conMod) + level * 5;
        state.setMaxHp(hp);
        state.setCurrentHp(hp);

        // 生命骰信息
        state.setHitDie(hitDie);
        state.setMaxHitDice(level);
        state.setHitDice(level); // 短休前满

        // AC 计算 — 从 character_inventory 读取已装备护甲
        int ac = 10 + state.getDexMod(); // 无甲基础 AC = 10 + DEX_mod
        try {
            List<CharacterInventory> equippedArmor = inventoryService.list(new LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, state.getCharacterId())
                    .eq(CharacterInventory::getIsEquipped, true)
                    .eq(CharacterInventory::getSlot, "ARMOR"));
            if (equippedArmor != null && !equippedArmor.isEmpty()) {
                String armorItemId = equippedArmor.get(0).getItemId();
                MagicItem armorItem = magicItemService.getById(armorItemId);
                if (armorItem != null && armorItem.getDetails() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> details = mapper.readValue(armorItem.getDetails(), Map.class);
                    Object acBonusObj = details.get("acBonus");
                    if (acBonusObj != null) {
                        int acBonus = ((Number) acBonusObj).intValue();
                        // 根据护甲类型决定 DEX 加成
                        String tags = armorItem.getTags();
                        boolean isHeavy = tags != null && tags.contains("重型");
                        boolean isMedium = tags != null && tags.contains("中型");
                        if (isHeavy) {
                            ac = 10 + acBonus; // 重甲：不加 DEX
                        } else if (isMedium) {
                            ac = 10 + acBonus + Math.min(2, state.getDexMod()); // 中甲：DEX 最多 +2
                        } else {
                            ac = 10 + acBonus + state.getDexMod(); // 轻甲：全额 DEX
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        state.setAc(ac);

        Map<Integer, Integer> slots = getSpellSlotsForLevel(level);
        state.setMaxSpellSlots(new HashMap<>(slots));
        state.setSpellSlots(new HashMap<>(slots));
    }

    private int getHitDie(String className) {
        if (className == null) return 8;
        String n = className.toLowerCase();
        if (n.contains("野蛮") || n.contains("barbar")) return 12;
        // 注意：游荡者(d8)必须在游侠(d10)之前检查，因为“游荡者”也包含“游”
        if (n.contains("荡") || n.contains("rogue")) return 8;
        if (n.contains("战士") || n.contains("fighter") || n.contains("圣") || n.contains("palad") || n.contains("游") || n.contains("rang")) return 10;
        if (n.contains("术") || n.contains("sorcer") || n.contains("wizard") || n.contains("法")) return 6;
        return 8; // bard, cleric, druid, monk, warlock
    }

    private Map<Integer, Integer> getSpellSlotsForLevel(int level) {
        Map<Integer, Integer> slots = new HashMap<>();
        int idx = Math.min(level, SPELL_SLOT_TABLE.length) - 1;
        if (idx < 0) idx = 0;
        int[] row = SPELL_SLOT_TABLE[idx];
        for (int i = 0; i < row.length; i++) {
            if (row[i] > 0) slots.put(i + 1, row[i]);
        }
        return slots;
    }

    private int getXpToNext(int level) {
        if (level >= XP_THRESHOLDS.length) return 999999;
        return XP_THRESHOLDS[level];
    }

    // ── 攻击加值（已迁移到 AttackTool）──

    /**
     * 从数据库获取玩家装备的武器
     */
    private Weapon getPlayerWeapon(AdventureState state) {
        try {
            PlayerCharacter pc = characterService.getById(state.getCharacterId());
            if (pc != null && pc.getWeaponId() != null && !pc.getWeaponId().isBlank()) {
                return weaponService.getById(pc.getWeaponId());
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── 骰子（已迁移到 DiceTool，保留薄包装供少数遗留调用）──

    private int rollD20() {
        return diceTool.rollD20();
    }

    private int rollDice(int count, int sides) {
        return diceTool.rollDice(count, sides);
    }

    // ── 法术伤害（已迁移到 DamageTool.rollSpellDamage）──

    // ── 法术匹配（已迁移到 SpellTool）──

    // ── 怪物解析（已迁移到 EncounterTool）──

    // ── 状态字符串 ──

    private String buildEnemyDescription(List<AdventureState.Enemy> enemies) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AdventureState.Enemy e : enemies) {
            counts.merge(e.getName(), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            parts.add(e.getValue() + "只" + e.getKey());
        }
        return String.join("、", parts);
    }

    private String buildEnemyStatus(AdventureState state) {
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (AdventureState.Enemy e : state.getCombat().getEnemies()) {
            sb.append(e.getName()).append("#").append(idx);
            if (e.isAlive()) {
                sb.append(": ").append(e.getHp()).append("/").append(e.getMaxHp()).append("HP\n");
            } else {
                sb.append(": 死亡\n");
            }
            idx++;
        }
        return sb.toString();
    }

    private String formatSpellSlots(AdventureState state) {
        if (state.getSpellSlots() == null || state.getSpellSlots().isEmpty()) return "无";
        List<String> parts = new ArrayList<>();
        for (int lv = 1; lv <= 9; lv++) {
            Integer cur = state.getSpellSlots().get(lv);
            Integer max = state.getMaxSpellSlots().get(lv);
            if (max != null && max > 0) {
                parts.add(lv + "环(" + (cur != null ? cur : 0) + "/" + max + ")");
            }
        }
        return parts.isEmpty() ? "无" : String.join(" ", parts);
    }

    // ── 法术加载 ──

    /**
     * 检查法术是否属于指定职业（通过 class_ids JSON 字段）
     */
    @SuppressWarnings("rawtypes")
    private boolean spellMatchesClass(Spell spell, String classId) {
        if (classId == null || classId.isBlank()) return true; // 无职业限制则允许
        String classIdsJson = spell.getClassIds();
        if (classIdsJson == null || classIdsJson.isBlank()) return true; // 无职业限制
        try {
            List<String> ids = mapper.readValue(classIdsJson, List.class);
            return ids.contains(classId);
        } catch (Exception e) {
            return true; // 解析失败则不限制
        }
    }

    @SuppressWarnings("rawtypes")
    private List<String> resolveSpells(String spellIdsJson) {
        List<String> result = new ArrayList<>();
        if (spellIdsJson == null || spellIdsJson.isBlank()) return result;
        try {
            List<String> ids = mapper.readValue(spellIdsJson, List.class);
            for (String id : ids) {
                Spell spell = spellService.getById(id);
                if (spell != null) {
                    String summary = spell.getSummary() != null ? spell.getSummary() : "";
                    result.add(spell.getName() + ": " + summary);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    // ── 背包系统 ──

    /**
     * 从数据库加载角色背包到 AdventureState（含 magic_item 详情）
     */
    private List<AdventureState.InventoryItem> loadInventory(String characterId) {
        List<AdventureState.InventoryItem> result = new ArrayList<>();
        try {
            List<CharacterInventory> dbItems = inventoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, characterId)
            );

            // 批量查询 magic_item 填充物品详情
            Map<String, MagicItem> magicItemMap = new HashMap<>();
            if (!dbItems.isEmpty()) {
                List<String> itemIds = new ArrayList<>();
                for (CharacterInventory dbItem : dbItems) {
                    if (dbItem.getItemId() != null && !dbItem.getItemId().isBlank()) {
                        itemIds.add(dbItem.getItemId());
                    }
                }
                if (!itemIds.isEmpty()) {
                    List<MagicItem> magicItems = magicItemService.list(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MagicItem>()
                            .in(MagicItem::getId, itemIds));
                    for (MagicItem mi : magicItems) {
                        magicItemMap.put(mi.getId(), mi);
                    }
                }
            }

            for (CharacterInventory dbItem : dbItems) {
                AdventureState.InventoryItem invItem = new AdventureState.InventoryItem(
                    dbItem.getId(),
                    dbItem.getItemType(),
                    dbItem.getItemId(),
                    dbItem.getItemName(),
                    dbItem.getQuantity() != null ? dbItem.getQuantity() : 1,
                    dbItem.getIsEquipped() != null && dbItem.getIsEquipped()
                );
                invItem.setSlot(dbItem.getSlot());

                // 从 magic_item 填充详情
                MagicItem mi = magicItemMap.get(dbItem.getItemId());
                if (mi != null) {
                    if (invItem.getItemName() == null || invItem.getItemName().isBlank()) {
                        invItem.setItemName(mi.getName());
                    }
                    if (invItem.getItemType() == null || invItem.getItemType().isBlank()) {
                        invItem.setItemType(mi.getItemType());
                    }
                    invItem.setRarity(mi.getRarity());
                    invItem.setDetails(mi.getDetails());
                    invItem.setSummary(mi.getSummary());
                }
                result.add(invItem);
            }
        } catch (Exception ignored) {}
        return result;
    }

    /**
     * 将战利品写入角色背包
     * 使用 lootItem.getItemId()（真实 magic_item.id）而非名称生成 ID
     */
    private void addLootToInventory(AdventureState state, List<AdventureState.LootItem> loot) {
        for (AdventureState.LootItem lootItem : loot) {
            // 金币不入背包
            if ("金币".equals(lootItem.getName())) continue;

            // 使用真实的 magic_item.id
            String itemId = lootItem.getItemId();
            if (itemId == null || itemId.isBlank()) {
                itemId = lootItem.getName().toLowerCase().replace(" ", "-");
            }

            // 确定物品类型（优先使用 lootItem 中的类型，否则从数据库查询）
            String itemType = lootItem.getItemType();
            String itemName = lootItem.getName();
            if (itemType == null || itemType.isBlank()) {
                try {
                    MagicItem mi = magicItemService.getById(itemId);
                    if (mi != null) {
                        itemType = mi.getItemType();
                        if (itemName == null || itemName.isBlank()) {
                            itemName = mi.getName();
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (itemType == null) itemType = "MAGIC_ITEM";

            // 查看背包是否已有同类物品
            boolean found = false;
            for (AdventureState.InventoryItem invItem : state.getInventory()) {
                if (invItem.getItemId().equals(itemId)) {
                    invItem.setQuantity(invItem.getQuantity() + lootItem.getQuantity());
                    found = true;
                    break;
                }
            }

            if (!found) {
                String newId = state.getCharacterId() + "_" + itemId + "_" + System.currentTimeMillis();
                AdventureState.InventoryItem newInv = new AdventureState.InventoryItem(
                    newId, itemType, itemId, itemName, lootItem.getQuantity(), false
                );
                newInv.setRarity(lootItem.getRarity());
                // 从 magic_item 获取 details
                try {
                    MagicItem mi = magicItemService.getById(itemId);
                    if (mi != null) {
                        newInv.setDetails(mi.getDetails());
                        newInv.setSummary(mi.getSummary());
                        if (itemName == null || itemName.isBlank()) {
                            newInv.setItemName(mi.getName());
                        }
                    }
                } catch (Exception ignored) {}
                state.getInventory().add(newInv);
            }

            // 写入数据库
            try {
                inventoryService.addItemToInventory(state.getCharacterId(), itemId, lootItem.getQuantity());
            } catch (Exception ignored) {}
        }
    }

    /**
     * 将单个背包物品保存到数据库（新增或更新）
     */
    private void saveInventoryItemToDb(String characterId, String itemType, String itemId,
                                        String itemName, int quantity) {
        try {
            var existing = inventoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, characterId)
                    .eq(CharacterInventory::getItemId, itemId)
            );
            if (existing != null && !existing.isEmpty()) {
                CharacterInventory inv = existing.get(0);
                inv.setQuantity(inv.getQuantity() + quantity);
                inventoryService.updateById(inv);
            } else {
                CharacterInventory inv = new CharacterInventory();
                inv.setId(characterId + "_" + itemId + "_" + System.currentTimeMillis());
                inv.setCharacterId(characterId);
                inv.setItemType(itemType);
                inv.setItemId(itemId);
                inv.setItemName(itemName);
                inv.setQuantity(quantity);
                inv.setIsEquipped(false);
                inventoryService.save(inv);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 更新数据库中背包物品数量（使用消耗品后）
     */
    private void updateInventoryInDb(String characterId, String itemId, int remainingQuantity) {
        try {
            var existing = inventoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, characterId)
                    .eq(CharacterInventory::getItemId, itemId)
            );
            if (existing != null && !existing.isEmpty()) {
                if (remainingQuantity <= 0) {
                    inventoryService.removeById(existing.get(0).getId());
                } else {
                    existing.get(0).setQuantity(remainingQuantity);
                    inventoryService.updateById(existing.get(0));
                }
            }
        } catch (Exception ignored) {}
    }

    // ── 通用工具 ──

    @SuppressWarnings("rawtypes")
    private String resolveName(String id, com.baomidou.mybatisplus.extension.service.IService service) {
        if (id == null || id.isBlank()) return "未知";
        try {
            Object entity = service.getById(id);
            if (entity instanceof Race r) return r.getName();
            if (entity instanceof Class c) return c.getName();
        } catch (Exception ignored) {}
        return id;
    }

    private void trimHistory(AdventureState state) {
        while (state.getHistory().size() > MAX_HISTORY * 2) {
            state.getHistory().remove(0);
        }
    }

    private void saveState(AdventureState state) {
        try {
            String json = mapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + state.getSessionId(),
                    json,
                    SESSION_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception ignored) {}
    }

    public AdventureState loadState(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sessionId);
            if (json == null) return null;
            return mapper.readValue(json, AdventureState.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
            emitter.complete();
        } catch (IOException ignored) {}
    }

    // ── DTO ──

    public static class ParsedResult {
        public String narrative;
        public List<String> choices;
        public String location;
    }

    private static class CombatResolution {
        String summary;
        boolean fled;
    }
}
