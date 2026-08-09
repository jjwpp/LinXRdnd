package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 职业等级成长配置
 * <p>
 * 每个 (class_id, level) 组合对应一条升级奖励。
 * reward_type: ABILITY / ASI / NEW_SPELL / COMBAT_STYLE / FEAT_CHOICE
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("class_level_progression")
public class ClassLevelProgression implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 职业ID (fighter / barbarian / ... ) */
    private String classId;

    /** 目标等级 (2-12) */
    private Integer level;

    /** 奖励类型: ABILITY / ASI / NEW_SPELL / COMBAT_STYLE / FEAT_CHOICE */
    private String rewardType;

    /** 奖励名称 */
    private String rewardName;

    /** 描述 */
    private String description;

    /** 详细数据 JSON */
    private String rewardData;

    /** 同等级内排序 */
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
