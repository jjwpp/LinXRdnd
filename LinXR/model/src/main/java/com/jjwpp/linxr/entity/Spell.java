package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("spell")
public class Spell implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String subtitle;

    private String summary;

    @TableField("`level`")
    private Integer level;

    private String school;

    private String castingTime;

    @TableField("`range`")
    private String range;

    private String components;

    private String duration;

    /** 关联职业ID列表，JSON数组格式如 ["wizard","sorcerer"] */
    private String classIds;

    private String tags;

    private String details;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
