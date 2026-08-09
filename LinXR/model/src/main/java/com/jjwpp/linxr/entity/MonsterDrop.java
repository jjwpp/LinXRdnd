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
 * 怪物掉落配置表
 * <p>
 * 配置每种怪物死亡后可能掉落的物品及概率。
 * 战斗胜利后，BattleService 读取此表进行随机掉落计算。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("monster_drop")
public class MonsterDrop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 关联 monster.id */
    private String monsterId;

    /** 关联 magic_item.id */
    private String itemId;

    /** 掉落概率 0.0 ~ 1.0 */
    private Double dropRate;

    /** 最小掉落数量 */
    private Integer minCount;

    /** 最大掉落数量 */
    private Integer maxCount;

    private LocalDateTime createdAt;

    // ═══ 非数据库字段：联表查询时填充 ═══

    /** 物品名称（来自 magic_item.name） */
    @TableField(exist = false)
    private String itemName;

    /** 物品类型（来自 magic_item.item_type） */
    @TableField(exist = false)
    private String itemType;

    /** 物品稀有度（来自 magic_item.rarity） */
    @TableField(exist = false)
    private String rarity;

    /** 怪物名称（来自 monster.name） */
    @TableField(exist = false)
    private String monsterName;
}
