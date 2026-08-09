package com.jjwpp.linxr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 昵称 */
    private String nickname;

    /** 用户名（登录用） */
    private String username;

    /** BCrypt 加密密码 */
    private String password;

    /** 头像URL */
    private String avatar;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
