package com.jjwpp.linxr.service.impl;

import com.jjwpp.linxr.entity.CharacterInventory;
import com.jjwpp.linxr.entity.MagicItem;
import com.jjwpp.linxr.mapper.CharacterInventoryMapper;
import com.jjwpp.linxr.service.ICharacterInventoryService;
import com.jjwpp.linxr.service.IMagicItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CharacterInventoryServiceImpl
        extends ServiceImpl<CharacterInventoryMapper, CharacterInventory>
        implements ICharacterInventoryService {

    @Autowired
    private IMagicItemService magicItemService;

    @Override
    public List<CharacterInventory> getInventoryWithDetails(String characterId) {
        List<CharacterInventory> items = this.list(new LambdaQueryWrapper<CharacterInventory>()
                .eq(CharacterInventory::getCharacterId, characterId)
                .orderByDesc(CharacterInventory::getCreatedAt));

        if (items == null || items.isEmpty()) {
            return items;
        }

        // 批量查询 magic_item 填充物品详情
        List<String> itemIds = items.stream()
                .map(CharacterInventory::getItemId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!itemIds.isEmpty()) {
            List<MagicItem> magicItems = magicItemService.list(
                    new LambdaQueryWrapper<MagicItem>().in(MagicItem::getId, itemIds));
            Map<String, MagicItem> itemMap = magicItems.stream()
                    .collect(Collectors.toMap(MagicItem::getId, m -> m, (a, b) -> a));

            for (CharacterInventory inv : items) {
                MagicItem mi = itemMap.get(inv.getItemId());
                if (mi != null) {
                    inv.setItemName(mi.getName());
                    inv.setItemType(mi.getItemType());
                    inv.setRarity(mi.getRarity());
                    inv.setDetails(mi.getDetails());
                    inv.setSummary(mi.getSummary());
                    inv.setSubtitle(mi.getSubtitle());
                }
            }
        }

        return items;
    }

    @Override
    public void addItemToInventory(String characterId, String itemId, int quantity) {
        // 查找是否已有该物品
        List<CharacterInventory> existing = this.list(new LambdaQueryWrapper<CharacterInventory>()
                .eq(CharacterInventory::getCharacterId, characterId)
                .eq(CharacterInventory::getItemId, itemId));

        if (existing != null && !existing.isEmpty()) {
            CharacterInventory inv = existing.get(0);
            inv.setQuantity((inv.getQuantity() != null ? inv.getQuantity() : 0) + quantity);
            this.updateById(inv);
        } else {
            CharacterInventory inv = new CharacterInventory();
            inv.setCharacterId(characterId);
            inv.setItemId(itemId);
            inv.setQuantity(quantity);
            inv.setIsEquipped(false);
            this.save(inv);
        }
    }

    @Override
    public int decrementItem(String characterId, String itemId, int amount) {
        List<CharacterInventory> existing = this.list(new LambdaQueryWrapper<CharacterInventory>()
                .eq(CharacterInventory::getCharacterId, characterId)
                .eq(CharacterInventory::getItemId, itemId));

        if (existing == null || existing.isEmpty()) {
            return -1;
        }

        CharacterInventory inv = existing.get(0);
        int currentQty = inv.getQuantity() != null ? inv.getQuantity() : 0;
        int remaining = currentQty - amount;

        if (remaining <= 0) {
            this.removeById(inv.getId());
            return 0;
        } else {
            inv.setQuantity(remaining);
            this.updateById(inv);
            return remaining;
        }
    }

    @Override
    public void addEquippedItem(String characterId, String itemId, String slot) {
        // 先卸下同槽位的已装备物品
        List<CharacterInventory> equipped = this.list(new LambdaQueryWrapper<CharacterInventory>()
                .eq(CharacterInventory::getCharacterId, characterId)
                .eq(CharacterInventory::getIsEquipped, true)
                .eq(CharacterInventory::getSlot, slot));
        for (CharacterInventory e : equipped) {
            e.setIsEquipped(false);
            e.setSlot(null);
            this.updateById(e);
        }

        // 检查背包是否已有该物品
        List<CharacterInventory> existing = this.list(new LambdaQueryWrapper<CharacterInventory>()
                .eq(CharacterInventory::getCharacterId, characterId)
                .eq(CharacterInventory::getItemId, itemId));

        if (existing != null && !existing.isEmpty()) {
            CharacterInventory inv = existing.get(0);
            inv.setIsEquipped(true);
            inv.setSlot(slot);
            this.updateById(inv);
        } else {
            CharacterInventory inv = new CharacterInventory();
            inv.setCharacterId(characterId);
            inv.setItemId(itemId);
            inv.setQuantity(1);
            inv.setIsEquipped(true);
            inv.setSlot(slot);
            this.save(inv);
        }
    }
}
