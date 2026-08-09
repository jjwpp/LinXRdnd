package com.jjwpp.linxr.dm;

import com.jjwpp.linxr.dm.tool.*;
import com.jjwpp.linxr.entity.*;
import com.jjwpp.linxr.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.*;


/**
 * 游戏工具集 — 供 AI Agent 通过 Function Calling 自主调用。
 * <p>
 * 每个方法对应一个游戏机制（攻击、施法、使用物品等），
 * AI 根据玩家输入和当前状态决定调用哪个工具。
 * 工具执行后返回结果字符串，AI 基于结果生成叙事。
 * <p>
 * 所有数值计算仍由底层 Tool 类（DiceTool/HpTool/AttackTool 等）完成，
 * GameTools 只是将它们封装为 AI 可调用的 @Tool 方法。
 */
public class GameTools {

    private final AdventureState state;
    private final DiceTool diceTool;
    private final HpTool hpTool;
    private final AttackTool attackTool;
    private final DamageTool damageTool;
    private final SpellTool spellTool;
    private final EncounterTool encounterTool;
    private final LootTool lootTool;
    private final AbilityTool abilityTool;
    private final ItemEffectProcessor itemEffectProcessor;
    private final IWeaponService weaponService;
    private final ICharacterInventoryService inventoryService;
    private final IMagicItemService magicItemService;
    private final IPlayerCharacterService playerCharacterService;
    private final ISpellService spellService;

    // XP 升级阈值（索引=当前等级，值=升到下一级所需XP）
    private static final int[] XP_THRESHOLDS = {
            0, 120, 350, 900, 2200, 5000, 12000, 23000, 34000, 48000, 64000,
            85000, 100000, 120000, 153000, 190000
    };

    // DND 5e 法术位表 [等级][环阶1-9]
    private static final int[][] SPELL_SLOT_TABLE = {
            {2, 0, 0, 0, 0, 0, 0, 0, 0},
            {3, 0, 0, 0, 0, 0, 0, 0, 0},
            {4, 2, 0, 0, 0, 0, 0, 0, 0},
            {4, 3, 0, 0, 0, 0, 0, 0, 0},
            {4, 3, 2, 0, 0, 0, 0, 0, 0},
            {4, 3, 3, 0, 0, 0, 0, 0, 0},
            {4, 3, 3, 1, 0, 0, 0, 0, 0},
            {4, 3, 3, 2, 0, 0, 0, 0, 0},
            {4, 3, 3, 3, 1, 0, 0, 0, 0},
    };

    public GameTools(
            AdventureState state,
            DiceTool diceTool,
            HpTool hpTool,
            AttackTool attackTool,
            DamageTool damageTool,
            SpellTool spellTool,
            EncounterTool encounterTool,
            LootTool lootTool,
            AbilityTool abilityTool,
            ItemEffectProcessor itemEffectProcessor,
            IWeaponService weaponService,
            ICharacterInventoryService inventoryService,
            IMagicItemService magicItemService,
            IPlayerCharacterService playerCharacterService,
            ISpellService spellService) {
        this.state = state;
        this.diceTool = diceTool;
        this.hpTool = hpTool;
        this.attackTool = attackTool;
        this.damageTool = damageTool;
        this.spellTool = spellTool;
        this.encounterTool = encounterTool;
        this.lootTool = lootTool;
        this.abilityTool = abilityTool;
        this.itemEffectProcessor = itemEffectProcessor;
        this.weaponService = weaponService;
        this.inventoryService = inventoryService;
        this.magicItemService = magicItemService;
        this.playerCharacterService = playerCharacterService;
        this.spellService = spellService;
    }

    // ════════════════════════════════════════════════════════════
    //  状态查询工具
    // ════════════════════════════════════════════════════════════

    @Tool("获取当前游戏状态，包括玩家HP、属性、法术位、背包、敌人信息等。在不确定当前状态时调用。")
    public String getGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前阶段】").append(state.getPhase()).append("\n");
        sb.append("【玩家】HP: ").append(state.getCurrentHp()).append("/").append(state.getMaxHp());
        sb.append(" | AC: ").append(state.getAc());
        sb.append(" | 等级: ").append(state.getLevel());
        sb.append(" | XP: ").append(state.getXp()).append("/").append(state.getXpToNext()).append("\n");
        sb.append("【属性】").append(abilityTool.formatAbilityScores(state)).append("\n");

        if (state.getSpellSlots() != null && !state.getSpellSlots().isEmpty()) {
            sb.append("【法术位】").append(formatSpellSlots()).append("\n");
        }

        if (state.getInventory() != null && !state.getInventory().isEmpty()) {
            sb.append("【背包】");
            for (AdventureState.InventoryItem item : state.getInventory()) {
                if (item.getQuantity() > 0) {
                    sb.append(item.getItemName()).append(" x").append(item.getQuantity());
                    if (item.isEquipped()) sb.append("[已装备]");
                    sb.append("，");
                }
            }
            sb.append("\n");
        }

        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null) {
            String phaseLabel = "ENEMY_TURN".equals(state.getCombat().getCombatPhase())
                    ? "敌人回合（等待执行）" : "玩家回合";
            sb.append("【战斗】回合: ").append(state.getCombat().getRound());
            sb.append(" | 行动点: ").append(state.getCombat().getActionPoints());
            sb.append(" | 当前阶段: ").append(phaseLabel).append("\n");
            sb.append("【敌人】");
            for (AdventureState.Enemy e : state.getCombat().getEnemies()) {
                sb.append(e.getName());
                if (e.isAlive()) {
                    sb.append("(HP:").append(e.getHp()).append("/").append(e.getMaxHp()).append(") ");
                } else {
                    sb.append("(已击败) ");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    //  战斗工具
    // ════════════════════════════════════════════════════════════

    @Tool("玩家对指定敌人进行近战攻击。返回攻击检定和伤害结果。仅在战斗中玩家回合可用。")
    public String playerMeleeAttack(
            @P("要攻击的敌人名称") String enemyName) {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return "错误：当前不在战斗中，无法进行近战攻击。";
        }
        if (!"PLAYER_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前不是你的回合，请等待敌人行动完成。";
        }

        AdventureState.Enemy target = findEnemy(enemyName);
        if (target == null) {
            return "错误：找不到名为「" + enemyName + "」的存活敌人。";
        }

        // 检查行动点
        if (state.getCombat().getActionPoints() <= 0) {
            return "错误：行动点已耗尽，请结束回合。";
        }

        // 获取已装备武器信息
        EquippedWeaponInfo weapon = getEquippedWeaponInfo();
        int weaponAtkBonus = weapon != null ? weapon.attackBonus : 0;

        // 攻击检定：熟练加值 + STR_mod + 武器攻击加值
        int prof = abilityTool.getProficiency(state.getLevel());
        AttackTool.AttackResult atkResult = attackTool.resolveAttack(
                prof + state.getStrMod() + weaponAtkBonus, target.getAc());

        if (!atkResult.hit) {
            // 消耗 1 点行动点（即使未命中也消耗）
            state.getCombat().setActionPoints(state.getCombat().getActionPoints() - 1);
            return String.format("未命中！掷骰: %d + %d = %d vs AC %d。%s的攻击被闪避了。",
                    atkResult.roll, state.getStrMod(), atkResult.totalAttack, atkResult.targetAc, target.getName());
        }

        // 计算伤害 — 使用装备武器的伤害骰
        int damage;
        String damageType;
        if (weapon != null) {
            int diceDmg = diceTool.rollFormula(weapon.damageDice);
            if (atkResult.crit) diceDmg += diceTool.rollFormula(weapon.damageDice); // 暴击：骰子翻倍
            damage = diceDmg + Math.max(0, state.getStrMod());
            damageType = weapon.damageType;
        } else {
            int diceDmg = diceTool.rollDice(1, 3);
            if (atkResult.crit) diceDmg *= 2;
            damage = diceDmg + Math.max(0, state.getStrMod());
            damageType = "钝击";
        }

        hpTool.applyDamageToEnemy(target, damage);
        // 消耗 1 点行动点
        state.getCombat().setActionPoints(state.getCombat().getActionPoints() - 1);

        StringBuilder sb = new StringBuilder();
        if (atkResult.crit) {
            sb.append("暴击命中！掷骰: 自然20。");
        } else {
            sb.append(String.format("命中！掷骰: %d + %d = %d vs AC %d。", atkResult.roll, state.getStrMod(), atkResult.totalAttack, atkResult.targetAc));
        }
        sb.append(String.format("造成 %d 点%s伤害。", damage, damageType));
        sb.append(String.format("%s剩余HP: %d/%d。", target.getName(), target.getHp(), target.getMaxHp()));
        if (!target.isAlive()) {
            sb.append(target.getName()).append("被击败！");
        }

        // 自动检测胜利：所有敌人被击败时立即结算，不切换到敌人回合
        String victoryResult = autoCheckVictory();
        if (victoryResult != null) {
            sb.append("\n").append(victoryResult);
            return sb.toString();
        }
        state.getCombat().setCombatPhase("ENEMY_TURN");
        return sb.toString();
    }

    @Tool("玩家对指定敌人进行远程攻击。返回攻击检定和伤害结果。仅在战斗中玩家回合可用。")
    public String playerRangedAttack(
            @P("要攻击的敌人名称") String enemyName) {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return "错误：当前不在战斗中，无法进行远程攻击。";
        }
        if (!"PLAYER_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前不是你的回合，请等待敌人行动完成。";
        }

        AdventureState.Enemy target = findEnemy(enemyName);
        if (target == null) {
            return "错误：找不到名为「" + enemyName + "」的存活敌人。";
        }

        // 检查行动点
        if (state.getCombat().getActionPoints() <= 0) {
            return "错误：行动点已耗尽，请结束回合。";
        }

        // 获取已装备武器信息
        EquippedWeaponInfo weapon = getEquippedWeaponInfo();
        int weaponAtkBonus = weapon != null ? weapon.attackBonus : 0;

        // 攻击检定：熟练加值 + DEX_mod + 武器攻击加值
        int prof = abilityTool.getProficiency(state.getLevel());
        AttackTool.AttackResult atkResult = attackTool.resolveAttack(
                prof + state.getDexMod() + weaponAtkBonus, target.getAc());

        if (!atkResult.hit) {
            state.getCombat().setActionPoints(state.getCombat().getActionPoints() - 1);
            return String.format("未命中！掷骰: %d + %d = %d vs AC %d。攻击偏了。",
                    atkResult.roll, state.getDexMod(), atkResult.totalAttack, atkResult.targetAc);
        }

        // 计算伤害 — 使用装备武器的伤害骰
        int damage;
        String damageType;
        if (weapon != null) {
            int diceDmg = diceTool.rollFormula(weapon.damageDice);
            if (atkResult.crit) diceDmg += diceTool.rollFormula(weapon.damageDice); // 暴击：骰子翻倍
            damage = diceDmg + Math.max(0, state.getDexMod());
            damageType = weapon.damageType;
        } else {
            int diceDmg = diceTool.rollDice(1, 4);
            if (atkResult.crit) diceDmg *= 2;
            damage = diceDmg + Math.max(0, state.getDexMod());
            damageType = "穿刺";
        }

        hpTool.applyDamageToEnemy(target, damage);
        state.getCombat().setActionPoints(state.getCombat().getActionPoints() - 1);

        StringBuilder sb = new StringBuilder();
        if (atkResult.crit) {
            sb.append("暴击命中！掷骰: 自然20。");
        } else {
            sb.append(String.format("命中！掷骰: %d + %d = %d vs AC %d。", atkResult.roll, state.getDexMod(), atkResult.totalAttack, atkResult.targetAc));
        }
        sb.append(String.format("造成 %d 点%s伤害。", damage, damageType));
        sb.append(String.format("%s剩余HP: %d/%d。", target.getName(), target.getHp(), target.getMaxHp()));
        if (!target.isAlive()) {
            sb.append(target.getName()).append("被击败！");
        }

        // 自动检测胜利
        String victoryResult = autoCheckVictory();
        if (victoryResult != null) {
            sb.append("\n").append(victoryResult);
            return sb.toString();
        }
        state.getCombat().setCombatPhase("ENEMY_TURN");
        return sb.toString();
    }

    @Tool("玩家施放法术。targetName为'self'时对自己施法（治疗等），为敌人名称时对该敌人施法。返回法术效果。")
    public String castSpell(
            @P("要施放的法术名称") String spellName,
            @P("目标名称，self表示自己，否则为敌人名称") String targetName) {
        if (!"COMBAT".equals(state.getPhase()) && !"EXPLORE".equals(state.getPhase())) {
            return "错误：当前阶段无法施法。";
        }
        // 战斗中检查是否为玩家回合
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null
                && !"PLAYER_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前不是你的回合，请等待敌人行动完成。";
        }

        SpellTool.SpellValidation spellVal = spellTool.validateSpell(state, spellName);
        if (!spellVal.success) {
            return "施法失败：" + spellVal.reason;
        }

        Spell spell = spellVal.spell;
        int slotLevelUsed = spellVal.slotLevelUsed;

        // 消耗法术位
        if (slotLevelUsed > 0) {
            spellTool.consumeSlot(state, slotLevelUsed);
        }

        DamageTool.DamageResult dmgResult = damageTool.rollSpellDamage(spell);
        boolean isHeal = spellTool.isHealSpell(spell);

        // 治疗法术
        if (isHeal && dmgResult.damage > 0) {
            int actualHeal = hpTool.applyHeal(state, dmgResult.damage);
            if ("COMBAT".equals(state.getPhase())) consumeActionPoint();
            return String.format("施放「%s」成功。%s恢复 %d 点HP。当前HP: %d/%d。%s",
                    spell.getName(),
                    slotLevelUsed > 0 ? "消耗" + slotLevelUsed + "环法术位。" : "",
                    actualHeal, state.getCurrentHp(), state.getMaxHp(),
                    spell.getSummary() != null ? spell.getSummary() : "");
        }

        // 非伤害法术
        if (dmgResult.damage == 0) {
            if ("COMBAT".equals(state.getPhase())) consumeActionPoint();
            return String.format("施放「%s」成功。%s%s",
                    spell.getName(),
                    slotLevelUsed > 0 ? "消耗" + slotLevelUsed + "环法术位。" : "",
                    spell.getSummary() != null ? spell.getSummary() : "效果生效。");
        }

        // 伤害法术 — 需要目标
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return String.format("施放「%s」成功，但当前不在战斗中，伤害法术无目标。%s",
                    spell.getName(), slotLevelUsed > 0 ? "消耗" + slotLevelUsed + "环法术位。" : "");
        }

        // 战斗中使用法术需要行动点
        if (state.getCombat().getActionPoints() <= 0) {
            return "错误：行动点已耗尽，请结束回合。";
        }

        boolean isMultiTarget = spellTool.isMultiTargetSpell(spell);
        boolean autoHit = spellTool.isAutoHitSpell(spell);
        List<AdventureState.Enemy> aliveEnemies = state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive).toList();

        if (aliveEnemies.isEmpty()) {
            consumeActionPoint();
            return "没有可攻击的目标。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("施放「%s」成功。%s", spell.getName(),
                slotLevelUsed > 0 ? "消耗" + slotLevelUsed + "环法术位。" : ""));

        // 多目标法术
        if (isMultiTarget) {
            sb.append("范围效果席卷战场！");
            for (AdventureState.Enemy target : aliveEnemies) {
                hpTool.applyDamageToEnemy(target, dmgResult.damage);
                sb.append(String.format(" %s受到%d点%s伤害(%d/%d)。", target.getName(),
                        dmgResult.damage, dmgResult.damageType, target.getHp(), target.getMaxHp()));
                if (!target.isAlive()) sb.append(target.getName()).append("被击败！");
            }
            consumeActionPoint();
            // 自动检测胜利
            String victoryResult = autoCheckVictory();
            if (victoryResult != null) {
                sb.append("\n").append(victoryResult);
                return sb.toString();
            }
            state.getCombat().setCombatPhase("ENEMY_TURN");
            return sb.toString();
        }

        // 单目标法术
        AdventureState.Enemy target = findEnemy(targetName);
        if (target == null) {
            target = aliveEnemies.get(0);
        }

        if (!autoHit) {
            AttackTool.AttackResult atkResult = attackTool.playerSpellAttack(state, target);
            if (!atkResult.hit) {
                sb.append(String.format("但被%s闪避了！掷骰: %d vs AC %d。", target.getName(), atkResult.totalAttack, atkResult.targetAc));
                consumeActionPoint();
                return sb.toString();
            }
            sb.append("精准命中！");
        } else {
            sb.append("魔力自动命中！");
        }

        hpTool.applyDamageToEnemy(target, dmgResult.damage);
        sb.append(String.format("对%s造成%d点%s伤害。%s剩余HP: %d/%d。",
                target.getName(), dmgResult.damage, dmgResult.damageType,
                target.getName(), target.getHp(), target.getMaxHp()));
        if (!target.isAlive()) {
            sb.append(target.getName()).append("被击败！");
        }
        consumeActionPoint();
        // 自动检测胜利
        String victoryResult = autoCheckVictory();
        if (victoryResult != null) {
            sb.append("\n").append(victoryResult);
            return sb.toString();
        }
        state.getCombat().setCombatPhase("ENEMY_TURN");
        return sb.toString();
    }

    @Tool("使用背包中的物品（药水、卷轴等消耗品）。返回使用效果。")
    public String useItem(
            @P("要使用的物品名称") String itemName) {
        if (state.getInventory() == null || state.getInventory().isEmpty()) {
            return "错误：背包为空。";
        }
        // 战斗中检查是否为玩家回合
        if ("COMBAT".equals(state.getPhase()) && state.getCombat() != null
                && !"PLAYER_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前不是你的回合，请等待敌人行动完成。";
        }

        AdventureState.InventoryItem target = null;
        for (AdventureState.InventoryItem item : state.getInventory()) {
            if (item.getQuantity() > 0 && item.getItemName() != null
                    && (item.getItemName().contains(itemName) || itemName.contains(item.getItemName()))) {
                target = item;
                break;
            }
        }

        if (target == null) {
            return "错误：背包中没有名为「" + itemName + "」的物品。";
        }

        // 治疗药水特殊处理（向后兼容）
        if ("consumable".equals(target.getItemType()) && target.getItemName().contains("治疗")) {
            int healAmount = diceTool.rollDice(2, 4) + 2;
            int actualHeal = hpTool.applyHeal(state, healAmount);
            target.setQuantity(target.getQuantity() - 1);
            if (target.getQuantity() <= 0) {
                state.getInventory().remove(target);
            }
            syncInventoryToDb(target);
            if ("COMBAT".equals(state.getPhase())) consumeActionPoint();
            return String.format("使用了「%s」。恢复 %d 点HP。当前HP: %d/%d。剩余数量: %d。",
                    target.getItemName(), actualHeal, state.getCurrentHp(), state.getMaxHp(),
                    Math.max(0, target.getQuantity()));
        }

        // 通过 ItemEffectProcessor 处理
        if (target.getDetails() != null && !target.getDetails().isBlank()) {
            ItemEffectProcessor.EffectResult result = itemEffectProcessor.process(state, target.getDetails());
            if (result.isSuccess()) {
                target.setQuantity(target.getQuantity() - 1);
                if (target.getQuantity() <= 0) {
                    state.getInventory().remove(target);
                }
                syncInventoryToDb(target);
                if ("COMBAT".equals(state.getPhase())) consumeActionPoint();
                // 战斗中使用物品可能击杀敌人（如伤害卷轴/炸弹），必须检测胜利
                String victoryResult = autoCheckVictory();
                if (victoryResult != null) {
                    return String.format("使用了「%s」。%s 剩余数量: %d。\n%s",
                            target.getItemName(), result.getMessage(), Math.max(0, target.getQuantity()), victoryResult);
                }
                return String.format("使用了「%s」。%s 剩余数量: %d。",
                        target.getItemName(), result.getMessage(), Math.max(0, target.getQuantity()));
            } else {
                return "使用失败：" + result.getMessage();
            }
        }

        if ("COMBAT".equals(state.getPhase())) consumeActionPoint();
        // 即使无效果也检测胜利（安全冗余）
        String victoryResult = autoCheckVictory();
        if (victoryResult != null) {
            return String.format("使用了「%s」，但没有效果。\n%s", target.getItemName(), victoryResult);
        }
        return String.format("使用了「%s」，但没有效果。", target.getItemName());
    }

    @Tool("尝试逃跑。进行逃跑检定，成功则脱离战斗，失败则浪费回合。仅在战斗中玩家回合可用。")
    public String attemptFlee() {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return "错误：当前不在战斗中，无需逃跑。";
        }
        if (!"PLAYER_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前不是你的回合，请等待敌人行动完成。";
        }

        int fleeDc = 10 + state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive)
                .mapToInt(AdventureState.Enemy::getXpReward)
                .max().orElse(50) / 50;
        int roll = diceTool.rollD20() + state.getDexMod();

        if (roll >= fleeDc) {
            state.setCombat(null);
            state.setPhase("EXPLORE");
            state.setExploreTurnCount(0);
            return String.format("逃跑成功！掷骰: %d + %d = %d vs DC %d。你脱离了战斗。", roll - state.getDexMod(), state.getDexMod(), roll, fleeDc);
        } else {
            consumeActionPoint();
            return String.format("逃跑失败！掷骰: %d + %d = %d vs DC %d。敌人拦住了你的去路。", roll - state.getDexMod(), state.getDexMod(), roll, fleeDc);
        }
    }

    @Tool("执行所有存活敌人的攻击回合。每个敌人都会攻击玩家。仅在玩家确认后调用（combatPhase为ENEMY_TURN时）。执行后回合数+1，回到玩家回合。")
    public String executeEnemyTurn() {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return "错误：当前不在战斗中。";
        }
        if (!"ENEMY_TURN".equals(state.getCombat().getCombatPhase())) {
            return "错误：当前仍是玩家回合，请先执行玩家行动。";
        }

        StringBuilder sb = new StringBuilder();
        for (AdventureState.Enemy enemy : state.getCombat().getEnemies()) {
            if (!enemy.isAlive()) continue;

            AttackTool.AttackResult atkResult = attackTool.enemyAttack(enemy, state.getAc());

            if (atkResult.hit) {
                int damage = diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice()) + enemy.getDamageBonus();
                if (atkResult.crit) damage += diceTool.rollDice(enemy.getDamageCount(), enemy.getDamageDice());
                hpTool.applyDamageToPlayer(state, damage);
                sb.append(String.format("%s命中你%s，造成%d点%s伤害。当前HP: %d/%d。",
                        enemy.getName(), atkResult.crit ? "（暴击！）" : "", damage,
                        enemy.getDamageType() != null ? enemy.getDamageType() : "物理",
                        state.getCurrentHp(), state.getMaxHp()));
            } else {
                sb.append(enemy.getName()).append("的攻击落空了。");
            }
            sb.append(" ");
        }

        // 回合数+1，重置行动点，回到玩家回合
        state.getCombat().setRound(state.getCombat().getRound() + 1);
        state.getCombat().setActionPoints(state.getCombat().getMaxActionPoints());
        state.getCombat().setCombatPhase("PLAYER_TURN");

        // 检查玩家死亡
        if (hpTool.isDead(state.getCurrentHp())) {
            state.setCurrentHp(0);
            state.setPhase("DEAD");
            state.setCombat(null);
            sb.append("你的生命值归零，倒在了血泊中...冒险到此结束。");
        }

        return sb.toString().trim();
    }

    // ════════════════════════════════════════════════════════════
    //  探索工具
    // ════════════════════════════════════════════════════════════

    @Tool("生成随机战斗遭遇，从怪物数据库按玩家等级筛选敌人。仅在探索阶段调用。触发后进入ENCOUNTER状态，等待玩家确认后进入战斗。")
    public String triggerEncounter() {
        if ("COMBAT".equals(state.getPhase()) || "ENCOUNTER".equals(state.getPhase())) {
            return "错误：已经在战斗或遭遇中，无法触发新遭遇。";
        }

        state.setExploreTurnCount(state.getExploreTurnCount() + 1);

        List<AdventureState.Enemy> enemies = encounterTool.generateEncounter(state.getLevel(), state.getLocation());
        if (enemies == null || enemies.isEmpty()) {
            return "没有生成遭遇，继续探索。";
        }

        // 创建战斗状态
        AdventureState.CombatState combat = new AdventureState.CombatState();
        combat.setRound(1);
        combat.setEnemies(enemies);
        state.setCombat(combat);
        // 进入 ENCOUNTER 状态（非直接 COMBAT），前端显示遭遇弹窗
        state.setPhase("ENCOUNTER");
        state.setExploreTurnCount(0);

        // 构建遭遇信息
        AdventureState.EncounterInfo encounterInfo = new AdventureState.EncounterInfo();
        encounterInfo.setEnemyCount(enemies.size());
        encounterInfo.setLocation(state.getLocation());
        for (AdventureState.Enemy e : enemies) {
            AdventureState.EncounterInfo.EnemyPreview preview = new AdventureState.EncounterInfo.EnemyPreview(
                    e.getName(), e.getHp(), e.getAc(), estimateCrFromXp(e.getXpReward()), e.getXpReward());
            preview.setDamageType(e.getDamageType());
            encounterInfo.getEnemies().add(preview);
        }
        state.setEncounterInfo(encounterInfo);

        StringBuilder sb = new StringBuilder("遭遇敌人：");
        for (AdventureState.Enemy e : enemies) {
            sb.append(e.getName()).append("(HP:").append(e.getHp()).append(") ");
        }
        sb.append("\n进入ENCOUNTER状态，等待玩家确认进入战斗。");
        return sb.toString().trim();
    }

    /** 根据 XP 推算 CR 字符串 */
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

    @Tool("长休，恢复全部HP和法术位。仅在探索阶段且玩家要求休息时调用。")
    public String longRest() {
        if ("COMBAT".equals(state.getPhase())) {
            return "错误：战斗中无法长休。";
        }

        state.setCurrentHp(state.getMaxHp());
        state.setSpellSlots(new HashMap<>(state.getMaxSpellSlots()));
        state.setHitDice(Math.max(1, state.getMaxHitDice() / 2));

        return String.format("长休完成。HP恢复至 %d/%d，法术位已恢复，生命骰恢复至 %d。",
                state.getCurrentHp(), state.getMaxHp(), state.getHitDice());
    }

    // ════════════════════════════════════════════════════════════
    //  战斗结算工具
    // ════════════════════════════════════════════════════════════

    @Tool("检查所有敌人是否已被击败。如果战斗胜利，计算XP、生成掉落、检查升级。战斗中每次攻击后都应调用。")
    public String checkVictory() {
        if (!"COMBAT".equals(state.getPhase()) || state.getCombat() == null) {
            return "当前不在战斗中。";
        }

        boolean allDead = state.getCombat().getEnemies().stream()
                .noneMatch(AdventureState.Enemy::isAlive);

        if (!allDead) {
            // 返回存活敌人状态
            List<String> alive = state.getCombat().getEnemies().stream()
                    .filter(AdventureState.Enemy::isAlive)
                    .map(e -> e.getName() + "(HP:" + e.getHp() + ")")
                    .toList();
            return "战斗尚未结束。存活敌人：" + String.join("，", alive);
        }

        // ═══ 战斗胜利 ═══
        int totalXp = state.getCombat().getEnemies().stream()
                .mapToInt(AdventureState.Enemy::getXpReward).sum();
        state.setXp(state.getXp() + totalXp);

        // 生成战利品
        List<AdventureState.LootItem> loot = lootTool.generateLoot(state.getCombat().getEnemies());
        addLootToInventory(loot);

        // 构建结算结果
        AdventureState.CombatResult cr = new AdventureState.CombatResult();
        cr.setXpGained(totalXp);
        cr.setLoot(loot);
        state.setCombatResult(cr);
        state.setCombat(null);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("战斗胜利！获得 %d XP。", totalXp));

        if (loot != null && !loot.isEmpty()) {
            sb.append("获得物品：");
            for (AdventureState.LootItem item : loot) {
                sb.append(item.getName()).append(" x").append(item.getQuantity()).append("，");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        // 检查升级
        if (state.getXp() >= state.getXpToNext() && state.getLevel() < XP_THRESHOLDS.length) {
            int newLevel = state.getLevel() + 1;
            state.setLevel(newLevel);
            state.setXpToNext(XP_THRESHOLDS[Math.min(newLevel, XP_THRESHOLDS.length - 1)]);

            // 升级增加 HP
            int hpGain = state.getHitDie() / 2 + 1 + Math.max(0, state.getConMod()) + 5;
            state.setMaxHp(state.getMaxHp() + hpGain);
            state.setCurrentHp(state.getCurrentHp() + hpGain);

            // 更新法术位
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
            sb.append(String.format("升级！达到 %d 级！HP增加 %d，法术位已更新。", newLevel, hpGain));
        } else {
            state.setPhase("EXPLORE");
        }

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    //  私有辅助方法
    // ════════════════════════════════════════════════════════════

    private AdventureState.Enemy findEnemy(String name) {
        if (state.getCombat() == null || state.getCombat().getEnemies() == null) return null;
        for (AdventureState.Enemy e : state.getCombat().getEnemies()) {
            if (!e.isAlive()) continue;
            if (e.getName() != null && (e.getName().contains(name) || name.contains(e.getName()))) {
                return e;
            }
        }
        // 回退：第一个存活敌人
        return state.getCombat().getEnemies().stream()
                .filter(AdventureState.Enemy::isAlive)
                .findFirst().orElse(null);
    }

    /**
     * 战斗中消耗 1 点行动点（不再自动切换到敌人回合，由玩家点结束按钮触发）
     */
    private void consumeActionPoint() {
        if (state.getCombat() != null && state.getCombat().getActionPoints() > 0) {
            state.getCombat().setActionPoints(state.getCombat().getActionPoints() - 1);
        }
    }

    /**
     * 自动检测胜利：如果所有敌人都被击败，立即处理战斗结算。
     * 在每次玩家攻击/施法后调用，不依赖 AI 主动调 checkVictory()。
     * @return 胜利结算结果字符串，未胜利时返回 null
     */
    private String autoCheckVictory() {
        if (state.getCombat() == null) return null;
        boolean allDead = state.getCombat().getEnemies().stream()
                .noneMatch(AdventureState.Enemy::isAlive);
        if (!allDead) return null;

        // ═══ 战斗胜利 ═══
        int totalXp = state.getCombat().getEnemies().stream()
                .mapToInt(AdventureState.Enemy::getXpReward).sum();
        state.setXp(state.getXp() + totalXp);

        // 生成战利品
        List<AdventureState.LootItem> loot = lootTool.generateLoot(state.getCombat().getEnemies());
        addLootToInventory(loot);

        // 构建结算结果
        AdventureState.CombatResult cr = new AdventureState.CombatResult();
        cr.setXpGained(totalXp);
        cr.setLoot(loot);
        state.setCombatResult(cr);
        state.setCombat(null);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("战斗胜利！获得 %d XP。", totalXp));

        if (loot != null && !loot.isEmpty()) {
            sb.append("获得物品：");
            for (AdventureState.LootItem item : loot) {
                sb.append(item.getName()).append(" x").append(item.getQuantity()).append("，");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

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
            sb.append(String.format("升级！达到 %d 级！HP增加 %d，法术位已更新。", newLevel, hpGain));
        } else {
            state.setPhase("EXPLORE");
        }

        return sb.toString();
    }

    /**
     * 已装备武器信息（从 character_inventory 表读取 magic_item details JSON）
     */
    private static class EquippedWeaponInfo {
        final String name;
        final String damageDice;
        final String damageType;
        final int attackBonus;

        EquippedWeaponInfo(String name, String damageDice, String damageType, int attackBonus) {
            this.name = name;
            this.damageDice = damageDice;
            this.damageType = damageType;
            this.attackBonus = attackBonus;
        }
    }

    /**
     * 从 character_inventory 读取已装备的武器信息（slot=WEAPON, equipped=true）
     * 解析 magic_item.details JSON 获取 damageDice / damageType / attackBonus
     */
    @SuppressWarnings("unchecked")
    private EquippedWeaponInfo getEquippedWeaponInfo() {
        try {
            List<CharacterInventory> equipped = inventoryService.list(new LambdaQueryWrapper<CharacterInventory>()
                    .eq(CharacterInventory::getCharacterId, state.getCharacterId())
                    .eq(CharacterInventory::getIsEquipped, true)
                    .eq(CharacterInventory::getSlot, "WEAPON"));
            if (equipped == null || equipped.isEmpty()) return null;

            String weaponItemId = equipped.get(0).getItemId();
            MagicItem weaponItem = magicItemService.getById(weaponItemId);
            if (weaponItem == null || weaponItem.getDetails() == null) return null;

            ObjectMapper objMapper = new ObjectMapper();
            Map<String, Object> details = objMapper.readValue(weaponItem.getDetails(), Map.class);
            String damageDice = (String) details.getOrDefault("damageDice", "1d6");
            String damageType = (String) details.getOrDefault("damageType", "物理");
            int atkBonus = details.containsKey("attackBonus") ? ((Number) details.get("attackBonus")).intValue() : 0;

            return new EquippedWeaponInfo(weaponItem.getName(), damageDice, damageType, atkBonus);
        } catch (Exception e) {
            return null;
        }
    }

    private Weapon getPlayerWeapon() {
        try {
            PlayerCharacter pc = playerCharacterService.getById(state.getCharacterId());
            if (pc == null || pc.getWeaponId() == null) return null;
            return weaponService.getById(pc.getWeaponId());
        } catch (Exception e) {
            return null;
        }
    }

    private void addLootToInventory(List<AdventureState.LootItem> loot) {
        if (loot == null || loot.isEmpty()) return;
        for (AdventureState.LootItem item : loot) {
            if (item.getItemId() == null) continue; // 金币等无itemId的跳过

            // 更新数据库
            try {
                inventoryService.addItemToInventory(state.getCharacterId(), item.getItemId(), item.getQuantity());
            } catch (Exception ignored) {}

            // 更新运行时状态
            boolean found = false;
            for (AdventureState.InventoryItem inv : state.getInventory()) {
                if (item.getItemId().equals(inv.getItemId())) {
                    inv.setQuantity(inv.getQuantity() + item.getQuantity());
                    found = true;
                    break;
                }
            }
            if (!found) {
                AdventureState.InventoryItem inv = new AdventureState.InventoryItem(
                        UUID.randomUUID().toString(), item.getItemType(),
                        item.getItemId(), item.getName(), item.getQuantity(), false);
                inv.setRarity(item.getRarity());
                state.getInventory().add(inv);
            }
        }
    }

    private void syncInventoryToDb(AdventureState.InventoryItem item) {
        try {
            if (item.getItemId() == null) return;
            inventoryService.decrementItem(state.getCharacterId(), item.getItemId(), 1);
        } catch (Exception ignored) {}
    }

    private String formatSpellSlots() {
        if (state.getSpellSlots() == null || state.getSpellSlots().isEmpty()) return "无";
        StringBuilder sb = new StringBuilder();
        for (int lv = 1; lv <= 9; lv++) {
            Integer remaining = state.getSpellSlots().get(lv);
            Integer max = state.getMaxSpellSlots().get(lv);
            if (max != null && max > 0) {
                sb.append(lv).append("环:").append(remaining != null ? remaining : 0).append("/").append(max).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
