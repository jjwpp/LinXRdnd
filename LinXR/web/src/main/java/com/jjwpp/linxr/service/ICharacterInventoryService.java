package com.jjwpp.linxr.service;

import com.jjwpp.linxr.entity.CharacterInventory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ICharacterInventoryService extends IService<CharacterInventory> {

    /**
     * 获取角色背包物品列表（联表查询 magic_item 填充名称/类型/稀有度/效果）
     */
    List<CharacterInventory> getInventoryWithDetails(String characterId);

    /**
     * 添加物品到角色背包（如已存在则增加数量）
     */
    void addItemToInventory(String characterId, String itemId, int quantity);

    /**
     * 减少背包物品数量（数量归零则删除记录）
     * @return 剩余数量，-1 表示物品不存在
     */
    int decrementItem(String characterId, String itemId, int amount);

    /**
     * 添加已装备的物品到角色背包（自动设置 equipped=true 和 slot）
     * 如果该槽位已有装备，先卸下旧装备再装备新物品
     *
     * @param characterId 角色ID
     * @param itemId      物品ID（magic_item.id）
     * @param slot        装备槽位（WEAPON / ARMOR / HELMET / RING / AMULET）
     */
    void addEquippedItem(String characterId, String itemId, String slot);
}
