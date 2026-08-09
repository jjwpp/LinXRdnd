package com.jjwpp.linxr.dm;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 冒险会话状态（存储在 Redis 中，不落库）
 * <p>
 * 设计原则：AI 负责创意（旁白/场景），代码负责确定性数据（HP/骰子/XP/法术位）。
 */
@Data
public class AdventureState {

    private String sessionId;
    private String characterId;

    // 角色摘要（供 AI 使用，启动时一次性解析）
    private String characterName;
    private String raceName;
    private String className;
    private Integer level;
    private String summary;

    // ═══ DND 5e 六维属性 ═══
    private int strength = 10;
    private int dexterity = 10;
    private int constitution = 10;
    private int intelligence = 10;
    private int wisdom = 10;
    private int charisma = 10;

    // 属性调整值（由属性值计算，缓存供 Tool 使用）
    private int strMod;  // 力量调整值
    private int dexMod;  // 敏捷调整值
    private int conMod;  // 体质调整值
    private int intMod;  // 智力调整值
    private int wisMod;  // 感知调整值
    private int chaMod;  // 魅力调整值

    // 生命骰（用于短休恢复）
    private int hitDie;      // 面数（如 d8 = 8）
    private int hitDice;     // 剩余可用生命骰数量（短休时消耗）
    private int maxHitDice;  // 生命骰上限（= 角色等级）

    // 角色可用法术（"法术名: 简述" 格式，供 AI 生成施法选项）
    private List<String> spells = new ArrayList<>();

    // 角色背包物品（供 AI 和前端展示）
    private List<InventoryItem> inventory = new ArrayList<>();

    // 当前场景位置
    private String location;

    // 对话历史（role: user / assistant）
    private List<HistoryEntry> history = new ArrayList<>();

    // 当前可选行动
    private List<String> currentChoices = new ArrayList<>();

    // ════════════════════════════════════════════════════════════
    //  运行时状态（代码管理，AI 不直接修改）
    // ════════════════════════════════════════════════════════════

    /** 游戏阶段
     * EXPLORE - 探索状态
     * ENCOUNTER - 遭遇状态（发现敌人，显示遭遇弹窗，等待玩家确认进入战斗）
     * COMBAT - 战斗状态（回合制战斗中）
     * VICTORY - 胜利状态（战斗胜利结算）
     * DEFEAT - 失败状态（角色死亡）
     * LEVELUP - 升级状态（选择新专长和法术）
     */
    private String phase = "EXPLORE"; // EXPLORE | ENCOUNTER | COMBAT | VICTORY | DEFEAT | LEVELUP

    /** 玩家生命值 */
    private int currentHp;
    private int maxHp;

    /** 护甲等级 */
    private int ac;

    /** 经验值与升级阈值 */
    private int xp;
    private int xpToNext;

    /** 探索回合计数（用于控制战斗触发节奏） */
    private int exploreTurnCount;

    /** 法术位：key=法术环阶(1-9)，value=剩余数量 */
    private Map<Integer, Integer> spellSlots = new HashMap<>();
    /** 法术位上限（长休恢复到此值） */
    private Map<Integer, Integer> maxSpellSlots = new HashMap<>();

    /** 战斗状态（仅 COMBAT 阶段非空） */
    private CombatState combat;

    /** 待处理的升级选择（仅 LEVELUP 阶段非空） */
    private LevelUpChoices levelUpChoices;

    /** 战斗结算结果（仅战斗刚结束时设置，玩家确认后清除） */
    private CombatResult combatResult;

    /** 遭遇信息（仅 ENCOUNTER 阶段非空，包含敌人预览数据供前端展示遭遇弹窗） */
    private EncounterInfo encounterInfo;

    // ════════════════════════════════════════════════════════════
    //  内部类
    // ════════════════════════════════════════════════════════════

    @Data
    public static class HistoryEntry {
        private String role;    // "user" | "assistant"
        private String content;

        public HistoryEntry() {}

        public HistoryEntry(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /** 战斗状态 */
    @Data
    public static class CombatState {
        private int round = 1;
        private List<Enemy> enemies = new ArrayList<>();
        private String lastSummary; // 上一回合的战斗摘要，供 AI 连贯叙述
        /** 当前回合可用行动点（DND 5e: 标准1动作 + 1附赠动作，简化为行动点制） */
        private int actionPoints = 1;
        /** 每回合最大行动点 */
        private int maxActionPoints = 1;
        /** 战斗子阶段：PLAYER_TURN=等待玩家行动，ENEMY_TURN=等待玩家确认后执行敌人回合 */
        private String combatPhase = "PLAYER_TURN";
    }

    /** 敌人 */
    @Data
    public static class Enemy {
        private String monsterId;  // 关联 monster.id（用于掉落查询）
        private String name;
        private String imageUrl;   // 怪物立绘图片 URL（来自 MinIO）
        private int hp;
        private int maxHp;
        private int ac;
        private int attackBonus;   // 攻击加值
        private int damageDice;    // 伤害骰子面数 (如 6 = d6)
        private int damageCount;   // 伤害骰子数量
        private int damageBonus;   // 伤害固定加值
        private String damageType; // 伤害类型（来自怪物数据库）
        private int xpReward;      // 击杀奖励经验
        private boolean alive = true;

        public Enemy() {}

        public Enemy(String name, int hp, int ac, int xpReward) {
            this.name = name;
            this.hp = hp;
            this.maxHp = hp;
            this.ac = ac;
            this.xpReward = xpReward;
        }
    }

    /** 升级待选项 */
    @Data
    public static class LevelUpChoices {
        private int newLevel;
        private String className;
        private List<LevelUpReward> rewards = new ArrayList<>();
        private List<Choice> availableSpells = new ArrayList<>();
        private List<Choice> availableFeats = new ArrayList<>();
        /** ASI 选项中可选的属性列表 */
        private List<String> asiOptions = new ArrayList<>();
    }

    /** 数据库读取的升级奖励条目 */
    @Data
    public static class LevelUpReward {
        private String id;
        private String rewardType;   // ABILITY / ASI / NEW_SPELL / COMBAT_STYLE / FEAT_CHOICE
        private String rewardName;
        private String description;
        /** 解析后的 reward_data JSON */
        private Map<String, Object> data;
        /** 是否需要玩家选择 */
        private boolean requiresChoice;
    }

    /** 通用选项（专长或法术） */
    @Data
    public static class Choice {
        private String id;
        private String name;
        private String summary;

        public Choice() {}

        public Choice(String id, String name, String summary) {
            this.id = id;
            this.name = name;
            this.summary = summary;
        }
    }

    /** 战斗结算结果 */
    @Data
    public static class CombatResult {
        private int xpGained;
        private List<LootItem> loot = new ArrayList<>();
        private boolean leveledUp;
        private int newLevel;
    }

    /**
     * 遭遇信息 — 在 ENCOUNTER 阶段供前端展示遭遇弹窗。
     * 包含敌人预览数据（名称、数量、HP、等级、危险等级等）。
     * 玩家点击"进入战斗"后，EncounterInfo 被清除，状态切换到 COMBAT。
     */
    @Data
    public static class EncounterInfo {
        /** 遭遇的敌人预览列表 */
        private List<EnemyPreview> enemies = new ArrayList<>();
        /** 遭遇描述（AI 生成的叙事文本） */
        private String description;
        /** 危险等级（基于敌人CR总和与玩家等级比较） */
        private String dangerLevel; // TRIVIAL / EASY / MEDIUM / HARD / DEADLY
        /** 总敌人数量 */
        private int enemyCount;
        /** 遭遇地点 */
        private String location;

        /** 单个敌人的预览信息 */
        @Data
        public static class EnemyPreview {
            private String name;
            private String monsterId;   // 关联 monster.id（供前端匹配图片）
            private String imageUrl;    // 怪物立绘图片 URL（来自 MinIO）
            private int hp;
            private int maxHp;
            private int ac;
            private String cr;         // 挑战等级（如 "1/4", "2"）
            private int level;          // 推算等级
            private String damageType;
            private int xpReward;

            public EnemyPreview() {}

            public EnemyPreview(String name, int hp, int ac, String cr, int xpReward) {
                this.name = name;
                this.hp = hp;
                this.maxHp = hp;
                this.ac = ac;
                this.cr = cr;
                this.xpReward = xpReward;
                this.level = estimateLevel(cr);
            }

            /** 根据 CR 推算等级显示 */
            private static int estimateLevel(String cr) {
                if (cr == null) return 1;
                try {
                    if (cr.contains("/")) {
                        String[] parts = cr.split("/");
                        double val = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                        return Math.max(1, (int) Math.ceil(val));
                    }
                    return Math.max(1, (int) Double.parseDouble(cr));
                } catch (Exception e) {
                    return 1;
                }
            }
        }
    }

    /** 战利品条目 */
    @Data
    public static class LootItem {
        private String name;
        private String itemId;     // 关联 magic_item.id
        private int quantity;
        private String icon;
        private String rarity;     // COMMON / UNCOMMON / RARE / EPIC / LEGENDARY
        private String itemType;   // POTION / WEAPON / ARMOR / MAGIC_ITEM 等

        public LootItem() {}

        public LootItem(String name, int quantity, String icon, String rarity) {
            this.name = name;
            this.quantity = quantity;
            this.icon = icon;
            this.rarity = rarity;
        }

        public LootItem(String name, String itemId, int quantity, String icon, String rarity, String itemType) {
            this.name = name;
            this.itemId = itemId;
            this.quantity = quantity;
            this.icon = icon;
            this.rarity = rarity;
            this.itemType = itemType;
        }
    }

    /** 背包物品条目 */
    @Data
    public static class InventoryItem {
        private String id;
        private String itemType;   // POTION / WEAPON / ARMOR / MAGIC_ITEM 等
        private String itemId;
        private String itemName;
        private int quantity;
        private boolean equipped;
        private String slot;       // 装备位置: WEAPON / ARMOR / HELMET / RING / AMULET
        private String rarity;     // 稀有度
        private String details;    // 效果 JSON
        private String summary;    // 物品描述

        public InventoryItem() {}

        public InventoryItem(String id, String itemType, String itemId, String itemName, int quantity, boolean equipped) {
            this.id = id;
            this.itemType = itemType;
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.equipped = equipped;
        }
    }
}
