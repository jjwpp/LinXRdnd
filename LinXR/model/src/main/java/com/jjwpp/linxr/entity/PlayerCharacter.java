package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("player_character")
public class PlayerCharacter implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String subtitle;

    private String summary;

    private String playerName;

    /** 关联用户ID */
    private String userId;

    private Integer level;

    private String raceId;

    private String classId;

    /** 性别: male / female */
    private String gender;

    /** 男性角色立绘 MinIO URL */
    private String maleImageUrl;

    /** 女性角色立绘 MinIO URL */
    private String femaleImageUrl;

    private String featIds;

    private String spellIds;

    /** 装备的武器ID */
    private String weaponId;

    /** 装备的护甲ID */
    private String armorId;

    // ═══ DND 5e 六维属性 ═══
    /** 力量 */
    private Integer strength;
    /** 敏捷 */
    private Integer dexterity;
    /** 体质 */
    private Integer constitution;
    /** 智力 */
    private Integer intelligence;
    /** 感知 */
    private Integer wisdom;
    /** 魅力 */
    private Integer charisma;

    private String tags;

    private String details;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
