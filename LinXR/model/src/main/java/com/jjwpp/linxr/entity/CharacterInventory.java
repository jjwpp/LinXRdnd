package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 角色背包物品
 * <p>
 * 记录角色拥有的物品，关联 magic_item 全局物品定义表。
 * 一个角色可以拥有多个物品（1:N），一个物品模板可以被多个角色拥有（1:N）。
 * <p>
 * 物品的名称、类型、稀有度、效果等属性均来自 magic_item 表，
 * 此表仅记录"谁拥有什么物品、拥有多少、是否装备、装备在哪个槽位"。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("character_inventory")
public class CharacterInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 关联 player_character.id */
    private String characterId;

    /** 关联 magic_item.id */
    private String itemId;

    /** 物品数量 */
    private Integer quantity;

    /** 是否已装备（DB列名: equipped） */
    @TableField("equipped")
    private Boolean isEquipped;

    /** 装备位置: WEAPON / ARMOR / HELMET / RING / AMULET */
    private String slot;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ═══ 兼容旧字段（DB中可能仍存在这些列，保留以防数据丢失） ═══

    /** 旧字段：物品类型（已迁移到 magic_item.item_type，保留兼容） */
    private String itemType;

    /** 旧字段：物品名称（已迁移到 magic_item.name，保留兼容） */
    private String itemName;

    /** 旧字段：获取时间 */
    private LocalDateTime obtainedAt;

    // ═══ 非数据库字段：联表查询时从 magic_item 填充 ═══

    /** 物品稀有度（来自 magic_item.rarity） */
    @TableField(exist = false)
    private String rarity;

    /** 物品效果详情 JSON（来自 magic_item.details） */
    @TableField(exist = false)
    private String details;

    /** 物品描述（来自 magic_item.summary） */
    @TableField(exist = false)
    private String summary;

    /** 物品副标题（来自 magic_item.subtitle） */
    @TableField(exist = false)
    private String subtitle;
}
