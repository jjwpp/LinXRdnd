package com.jjwpp.linxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjwpp.linxr.entity.User;
import com.jjwpp.linxr.mapper.UserMapper;
import com.jjwpp.linxr.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String TOKEN_PREFIX = "token:";
    private static final long CAPTCHA_TTL_SECONDS = 60;
    private static final long TOKEN_TTL_HOURS = 24;

    // ═══ 验证码 ═══

    @Override
    public Map<String, String> generateCaptcha() {
        // 生成 4 位字母数字验证码（排除易混淆字符）
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String captcha = sb.toString();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis，60s 过期
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId,
                captcha,
                CAPTCHA_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        return Map.of("captchaId", captchaId, "captcha", captcha);
    }

    // ═══ 注册 ═══

    @Override
    public Map<String, Object> register(String nickname, String username, String password,
                                         String captchaId, String captcha) {
        // 1. 校验验证码
        validateCaptcha(captchaId, captcha);

        // 2. 检查用户名是否已存在
        long count = this.count(new QueryWrapper<User>().eq("username", username));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 3. BCrypt 加密密码 → 入库
        User user = new User();
        user.setNickname(nickname);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        this.save(user);

        // 4. 自动登录：生成 token
        String token = createToken(user);

        return buildLoginResult(token, user);
    }

    // ═══ 登录 ═══

    @Override
    public Map<String, Object> login(String username, String password,
                                      String captchaId, String captcha) {
        // 1. 校验验证码
        validateCaptcha(captchaId, captcha);

        // 2. 查用户
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. BCrypt 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 token 存 Redis
        String token = createToken(user);

        return buildLoginResult(token, user);
    }

    // ═══ 获取当前用户 ═══

    @Override
    public Map<String, Object> getCurrentUser(String token) {
        User user = getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("登录已过期，请重新登录");
        }
        return buildLoginResult(token, user);
    }

    @Override
    public User getUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userId == null) {
            return null;
        }
        return this.getById(userId);
    }

    // ═══ 登出 ═══

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }

    // ═══ 内部方法 ═══

    private void validateCaptcha(String captchaId, String captcha) {
        if (captchaId == null || captchaId.isBlank() || captcha == null || captcha.isBlank()) {
            throw new RuntimeException("请输入验证码");
        }
        String stored = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
        if (stored == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!stored.equalsIgnoreCase(captcha)) {
            throw new RuntimeException("验证码错误");
        }
        // 验证成功后删除，防止重复使用
        redisTemplate.delete(CAPTCHA_PREFIX + captchaId);
    }

    private String createToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                user.getId(),
                TOKEN_TTL_HOURS,
                TimeUnit.HOURS
        );
        return token;
    }

    private Map<String, Object> buildLoginResult(String token, User user) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("username", user.getUsername());
        userInfo.put("avatar", user.getAvatar());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", userInfo);
        return result;
    }
}
