package com.jjwpp.linxr.service;

import com.jjwpp.linxr.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface IUserService extends IService<User> {

    /**
     * 生成验证码并存入 Redis（60s 过期）
     * @return { captchaId, captcha }
     */
    Map<String, String> generateCaptcha();

    /**
     * 注册：校验验证码 → 检查用户名 → BCrypt 加密 → 入库
     */
    Map<String, Object> register(String nickname, String username, String password,
                                  String captchaId, String captcha);

    /**
     * 登录：校验验证码 → 查用户 → BCrypt 校验 → 生成 token 存 Redis
     * @return { token, user: { id, nickname, username } }
     */
    Map<String, Object> login(String username, String password,
                               String captchaId, String captcha);

    /**
     * 根据 token 获取当前用户信息（从 Redis 校验）
     */
    Map<String, Object> getCurrentUser(String token);

    /**
     * 根据 token 获取 User 实体（供其他模块复用）
     * @return User 实体，token 无效时返回 null
     */
    User getUserByToken(String token);

    /**
     * 登出：删除 Redis 中的 token
     */
    void logout(String token);
}
