package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.dm.AdventureState;
import com.jjwpp.linxr.dm.DmService;
import com.jjwpp.linxr.dm.ItemEffectProcessor;
import com.jjwpp.linxr.entity.CharacterInventory;
import com.jjwpp.linxr.entity.MagicItem;
import com.jjwpp.linxr.entity.MonsterDrop;
import com.jjwpp.linxr.entity.PlayerCharacter;
import com.jjwpp.linxr.entity.User;
import com.jjwpp.linxr.service.ICharacterInventoryService;
import com.jjwpp.linxr.service.IMagicItemService;
import com.jjwpp.linxr.service.IMonsterDropService;
import com.jjwpp.linxr.service.IPlayerCharacterService;
import com.jjwpp.linxr.service.IUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 背包系统接口
 * <p>
 * 提供角色背包管理、物品使用、战斗掉落等功能。
 * 所有接口需要登录认证（Authorization: Bearer {token}）。
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private ICharacterInventoryService inventoryService;

    @Autowired
    private IMagicItemService magicItemService;

    @Autowired
    private IMonsterDropService monsterDropService;

    @Autowired
    private IPlayerCharacterService playerCharacterService;

    @Autowired
    private IUserService userService;

    @Autowired
    private DmService dmService;

    @Autowired
    private ItemEffectProcessor itemEffectProcessor;

    /**
     * 获取角色背包（含物品详情）
     * <p>
     * GET /api/inventory/{characterId}
     * <p>
     * 返回物品列表，每个物品包含：
     * - 背包记录信息（数量、是否装备、装备位置）
     * - 物品详情（名称、类型、稀有度、效果JSON、描述）
     */
    @GetMapping("/{characterId}")
    public R<Map<String, Object>> getInventory(
            @PathVariable String characterId,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);
        PlayerCharacter pc = playerCharacterService.getById(characterId);
        if (pc == null) {
            return R.fail("角色不存在");
        }
        // 校验归属
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权访问该角色");
        }

        List<CharacterInventory> items = inventoryService.getInventoryWithDetails(characterId);

        // 构建响应
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("characterId", characterId);
        response.put("characterName", pc.getName());

        List<Map<String, Object>> itemList = new ArrayList<>();
        int totalWeight = 0;
        for (CharacterInventory inv : items) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", inv.getId());
            itemMap.put("itemId", inv.getItemId());
            itemMap.put("itemName", inv.getItemName());
            itemMap.put("itemType", inv.getItemType());
            itemMap.put("rarity", inv.getRarity());
            itemMap.put("quantity", inv.getQuantity());
            itemMap.put("isEquipped", inv.getIsEquipped());
            itemMap.put("slot", inv.getSlot());
            itemMap.put("details", inv.getDetails());
            itemMap.put("summary", inv.getSummary());
            itemMap.put("subtitle", inv.getSubtitle());

            // 解析效果类型供前端显示
            if (inv.getDetails() != null && !inv.getDetails().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode detailsNode =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(inv.getDetails());
                    itemMap.put("effectType", detailsNode.has("effectType") ? detailsNode.get("effectType").asText() : null);
                    itemMap.put("actionCost", detailsNode.has("actionCost") ? detailsNode.get("actionCost").asInt() : 1);
                } catch (Exception ignored) {
                    itemMap.put("effectType", null);
                    itemMap.put("actionCost", 1);
                }
            } else {
                itemMap.put("effectType", null);
                itemMap.put("actionCost", 1);
            }

            itemList.add(itemMap);
            totalWeight += inv.getQuantity() != null ? inv.getQuantity() : 0;
        }
        response.put("items", itemList);
        response.put("totalItems", totalWeight);
        response.put("maxSlots", 30); // 背包最大格子数

        return R.ok(response);
    }

    /**
     * 使用物品（战斗中或探索中）
     * <p>
     * POST /api/inventory/use
     * <p>
     * Body: { "characterId": "xxx", "itemId": "yyy", "battleId": "sessionId" }
     * <p>
     * 流程：
     * 1. 校验角色归属
     * 2. 检查背包数量
     * 3. 读取 magic_item.details
     * 4. ItemEffectProcessor 处理效果
     * 5. 修改角色状态
     * 6. quantity - 1
     * 7. 消耗行动点（战斗中）
     *
     * @return 使用结果、角色变化、剩余数量、行动点变化
     */
    @PostMapping("/use")
    public R<Map<String, Object>> useItem(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);
        String characterId = (String) body.get("characterId");
        String itemId = (String) body.get("itemId");
        String sessionId = (String) body.get("battleId"); // battleId 即 adventure sessionId

        if (characterId == null || itemId == null) {
            return R.fail("characterId 和 itemId 不能为空");
        }

        // 校验角色归属
        PlayerCharacter pc = playerCharacterService.getById(characterId);
        if (pc == null) {
            return R.fail("角色不存在");
        }
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权操作该角色");
        }

        // 检查背包中是否有该物品
        List<CharacterInventory> invList = inventoryService.list(
                new LambdaQueryWrapper<CharacterInventory>()
                        .eq(CharacterInventory::getCharacterId, characterId)
                        .eq(CharacterInventory::getItemId, itemId));
        if (invList == null || invList.isEmpty()) {
            return R.fail("背包中没有此物品");
        }
        CharacterInventory inv = invList.get(0);
        if (inv.getQuantity() == null || inv.getQuantity() <= 0) {
            return R.fail("物品数量不足");
        }

        // 读取 magic_item 获取 details
        MagicItem magicItem = magicItemService.getById(itemId);
        if (magicItem == null) {
            return R.fail("物品定义不存在");
        }

        String detailsJson = magicItem.getDetails();

        // 处理物品效果
        Map<String, Object> result = new LinkedHashMap<>();
        String message;

        if (sessionId != null && !sessionId.isBlank()) {
            // 战斗/冒险会话中：通过 DmService 处理
            Map<String, Object> useResult = dmService.useItemWithProcessor(sessionId, itemId, itemEffectProcessor);
            if (useResult.containsKey("error")) {
                return R.fail((String) useResult.get("error"));
            }
            result.putAll(useResult);
            message = (String) useResult.getOrDefault("narrative", "使用了" + magicItem.getName());
        } else {
            // 非会话模式（直接使用，无战斗状态）
            // 创建临时状态处理效果
            result.put("narrative", "你使用了" + magicItem.getName() + "，但没有进行中的冒险会话。");
            message = "非冒险会话中，物品效果无法生效。";
        }

        // 减少背包数量
        int remaining = inventoryService.decrementItem(characterId, itemId, 1);

        result.put("itemName", magicItem.getName());
        result.put("remainingQuantity", remaining);
        result.put("message", message);

        return R.ok(result);
    }

    /**
     * 获取战斗掉落（战斗胜利后查询掉落详情）
     * <p>
     * GET /api/battle/drop/{battleId}
     * <p>
     * battleId 即 adventure sessionId。
     * 返回该会话最近一次战斗的掉落信息。
     */
    @GetMapping("/drop/{battleId}")
    public R<Map<String, Object>> getBattleDrops(
            @PathVariable String battleId,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);

        // 通过 DmService 获取会话状态
        AdventureState state = dmService.loadState(battleId);
        if (state == null) {
            return R.fail("战斗会话不存在或已过期");
        }

        // 校验角色归属
        PlayerCharacter pc = playerCharacterService.getById(state.getCharacterId());
        if (pc == null || pc.getUserId() == null || !pc.getUserId().equals(user.getId())) {
            return R.fail("无权访问该会话");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", battleId);
        response.put("characterId", state.getCharacterId());

        // 获取战斗结算结果中的掉落
        if (state.getCombatResult() != null && state.getCombatResult().getLoot() != null) {
            List<Map<String, Object>> lootList = new ArrayList<>();
            for (AdventureState.LootItem loot : state.getCombatResult().getLoot()) {
                Map<String, Object> lootMap = new LinkedHashMap<>();
                lootMap.put("name", loot.getName());
                lootMap.put("itemId", loot.getItemId());
                lootMap.put("quantity", loot.getQuantity());
                lootMap.put("icon", loot.getIcon());
                lootMap.put("rarity", loot.getRarity());
                lootMap.put("itemType", loot.getItemType());
                lootList.add(lootMap);
            }
            response.put("loot", lootList);
            response.put("xpGained", state.getCombatResult().getXpGained());
        } else {
            response.put("loot", Collections.emptyList());
            response.put("xpGained", 0);
        }

        return R.ok(response);
    }

    /**
     * 装备/卸下物品
     * <p>
     * POST /api/inventory/equip
     * Body: { "characterId": "xxx", "itemId": "yyy", "slot": "WEAPON", "equip": true }
     */
    @PostMapping("/equip")
    public R<Map<String, Object>> toggleEquip(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);
        String characterId = (String) body.get("characterId");
        String itemId = (String) body.get("itemId");
        String slot = (String) body.get("slot");
        boolean equip = body.get("equip") != null ? (Boolean) body.get("equip") : true;

        // 校验角色归属
        PlayerCharacter pc = playerCharacterService.getById(characterId);
        if (pc == null) {
            return R.fail("角色不存在");
        }
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权操作该角色");
        }

        // 查找背包中的物品
        List<CharacterInventory> invList = inventoryService.list(
                new LambdaQueryWrapper<CharacterInventory>()
                        .eq(CharacterInventory::getCharacterId, characterId)
                        .eq(CharacterInventory::getItemId, itemId));
        if (invList == null || invList.isEmpty()) {
            return R.fail("背包中没有此物品");
        }

        CharacterInventory inv = invList.get(0);

        if (equip) {
            // 装备：先卸下同槽位的其他物品
            List<CharacterInventory> equipped = inventoryService.list(
                    new LambdaQueryWrapper<CharacterInventory>()
                            .eq(CharacterInventory::getCharacterId, characterId)
                            .eq(CharacterInventory::getIsEquipped, true)
                            .eq(CharacterInventory::getSlot, slot));
            for (CharacterInventory e : equipped) {
                e.setIsEquipped(false);
                e.setSlot(null);
                inventoryService.updateById(e);
            }
            inv.setIsEquipped(true);
            inv.setSlot(slot);
        } else {
            inv.setIsEquipped(false);
            inv.setSlot(null);
        }

        inventoryService.updateById(inv);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemName", inv.getItemName());
        result.put("equipped", inv.getIsEquipped());
        result.put("slot", inv.getSlot());
        return R.ok(result);
    }

    // ═══ 内部方法 ═══

    private User resolveUser(String auth) {
        String token = extractToken(auth);
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }

    private String extractToken(String auth) {
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }
}
