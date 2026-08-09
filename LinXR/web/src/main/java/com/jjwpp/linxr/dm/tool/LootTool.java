package com.jjwpp.linxr.dm.tool;

import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.entity.MagicItem;
import com.jjwpp.linxr.entity.MonsterDrop;
import com.jjwpp.linxr.entity.Weapon;
import com.jjwpp.linxr.service.IArmorService;
import com.jjwpp.linxr.service.IMagicItemService;
import com.jjwpp.linxr.service.IMonsterDropService;
import com.jjwpp.linxr.service.IWeaponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 战利品生成工具 — 基于 monster_drop 配置表 + 通用掉落规则。
 * <p>
 * 数据来源：
 * 1. monster_drop 表：怪物专属掉落配置（精确掉落，按概率随机）
 * 2. 通用规则：金币 + 治疗药水 + 稀有物品（作为补充）
 * <p>
 * 战斗胜利后，BattleService 调用此工具生成掉落列表。
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class LootTool {

    @Autowired
    private IMagicItemService magicItemService;

    @Autowired
    private IWeaponService weaponService;

    @Autowired
    private IArmorService armorService;

    @Autowired
    private IMonsterDropService monsterDropService;

    /**
     * 根据击败的敌人列表生成战利品
     * 优先读取 monster_drop 表配置，补充通用掉落
     */
    public List<AdventureState.LootItem> generateLoot(List<AdventureState.Enemy> enemies) {
        List<AdventureState.LootItem> loot = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // ═══ 1. 通用掉落（金币 + 治疗药水） ═══
        int totalGold = 0;
        int potionChance = 0;
        boolean hasToughEnemy = false;

        for (AdventureState.Enemy e : enemies) {
            int xp = e.getXpReward();
            if (xp <= 40) {
                totalGold += rng.nextInt(1, 7);
                potionChance += 10;
            } else if (xp <= 150) {
                totalGold += rng.nextInt(2, 13);
                potionChance += 20;
            } else if (xp <= 700) {
                totalGold += rng.nextInt(5, 25);
                potionChance += 30;
                hasToughEnemy = true;
            } else {
                totalGold += rng.nextInt(10, 41);
                potionChance += 40;
                hasToughEnemy = true;
            }
        }

        if (totalGold > 0) {
            loot.add(new AdventureState.LootItem("金币", totalGold, "🪙", "COMMON"));
        }

        if (rng.nextInt(100) < potionChance) {
            loot.add(new AdventureState.LootItem("治疗药水", totalGold, "🧪", "UNCOMMON"));
        }

        // ═══ 2. monster_drop 表配置掉落 ═══
        for (AdventureState.Enemy e : enemies) {
            if (e.getMonsterId() == null || e.getMonsterId().isBlank()) continue;

            try {
                List<MonsterDrop> drops = monsterDropService.getDropsByMonsterId(e.getMonsterId());
                if (drops == null || drops.isEmpty()) continue;

                for (MonsterDrop drop : drops) {
                    double roll = rng.nextDouble();
                    if (roll < (drop.getDropRate() != null ? drop.getDropRate() : 0.0)) {
                        // 掉落成功，计算数量
                        int minC = drop.getMinCount() != null ? drop.getMinCount() : 1;
                        int maxC = drop.getMaxCount() != null ? drop.getMaxCount() : 1;
                        int count = minC >= maxC ? minC : rng.nextInt(minC, maxC + 1);

                        // 查询 magic_item 获取物品信息
                        MagicItem mi = magicItemService.getById(drop.getItemId());
                        if (mi != null) {
                            String rarity = mi.getRarity() != null ? mi.getRarity().toUpperCase() : "COMMON";
                            String icon = getIconForType(mi.getItemType(), rarity);
                            loot.add(new AdventureState.LootItem(
                                mi.getName(),
                                mi.getId(),
                                count,
                                icon,
                                rarity,
                                mi.getItemType()
                            ));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // ═══ 3. 稀有物品补充（如果 monster_drop 未产生物品） ═══
        boolean hasItemDrop = loot.stream().anyMatch(l -> l.getItemId() != null);
        if (!hasItemDrop && hasToughEnemy && rng.nextInt(100) < 30) {
            AdventureState.LootItem rareItem = generateRareLoot();
            if (rareItem != null) {
                loot.add(rareItem);
            }
        }

        return loot;
    }

    /**
     * 从数据库随机选取一个真实的魔法物品作为稀有战利品
     */
    private AdventureState.LootItem generateRareLoot() {
        try {
            List<MagicItem> magicItems = magicItemService.list();
            if (magicItems != null && !magicItems.isEmpty()) {
                MagicItem item = magicItems.get(ThreadLocalRandom.current().nextInt(magicItems.size()));
                String rarity = item.getRarity() != null ? item.getRarity().toUpperCase() : "RARE";
                String icon = getIconForType(item.getItemType(), rarity);
                return new AdventureState.LootItem(
                    item.getName(),
                    item.getId(),
                    1,
                    icon,
                    rarity,
                    item.getItemType()
                );
            }
        } catch (Exception ignored) {}

        // 数据库无魔法物品时，从武器表选取
        try {
            List<Weapon> weapons = weaponService.list();
            if (weapons != null && !weapons.isEmpty()) {
                Weapon w = weapons.get(ThreadLocalRandom.current().nextInt(weapons.size()));
                return new AdventureState.LootItem(w.getName(), null, 1, "⚔️", "UNCOMMON", "WEAPON");
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * 根据物品类型和稀有度返回图标
     */
    private String getIconForType(String itemType, String rarity) {
        if (itemType == null) return "💎";
        switch (itemType.toUpperCase()) {
            case "POTION": return "🧪";
            case "WEAPON": return "⚔️";
            case "ARMOR": return "🛡️";
            case "RING": return "💍";
            case "AMULET": return "📿";
            case "SCROLL": return "📜";
            case "WAND": return "🪄";
            default:
                if (rarity != null && (rarity.equals("LEGENDARY") || rarity.equals("EPIC"))) return "🏆";
                return "💎";
        }
    }
}
