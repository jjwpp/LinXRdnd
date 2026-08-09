package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author lxr
 * @since 2026-07-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("monster")
public class Monster implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String subtitle;

    private String summary;

    private String monsterType;

    private String size;

    private String cr;

    private Integer ac;

    private String hp;

    /** 攻击加值（来自数据库） */
    private Integer attackBonus;

    /** 伤害骰子公式，如 1d6+2（来自数据库） */
    private String damageFormula;

    /** 伤害类型（来自数据库） */
    private String damageType;

    /** MinIO 图片 URL */
    private String imageUrl;

    private String tags;

    private String details;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
