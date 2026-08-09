package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口
 * <p>
 * - POST /api/auth/captcha   获取验证码（Redis 存储 60s）
 * - POST /api/auth/register  注册（昵称 + 用户名 + 密码 + 验证码）
 * - POST /api/auth/login     登录（用户名 + 密码 + 验证码）
 * - GET  /api/auth/me        获取当前用户信息（需 token）
 * - POST /api/auth/logout    登出
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    /** 获取验证码 */
    @PostMapping("/captcha")
    public R<Map<String, String>> captcha() {
        return R.ok(userService.generateCaptcha());
    }

    /** 注册 */
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        String username = body.get("username");
        String password = body.get("password");
        String captchaId = body.get("captchaId");
        String captcha = body.get("captcha");

        if (nickname == null || nickname.isBlank()) return R.fail("昵称不能为空");
        if (username == null || username.isBlank()) return R.fail("用户名不能为空");
        if (password == null || password.length() < 6) return R.fail("密码至少6位");

        return R.ok(userService.register(nickname, username, password, captchaId, captcha));
    }

    /** 登录 */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaId = body.get("captchaId");
        String captcha = body.get("captcha");

        if (username == null || username.isBlank()) return R.fail("用户名不能为空");
        if (password == null || password.isBlank()) return R.fail("密码不能为空");

        return R.ok(userService.login(username, password, captchaId, captcha));
    }

    /** 获取当前登录用户 */
    @GetMapping("/me")
    public R<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        return R.ok(userService.getCurrentUser(token));
    }

    /** 登出 */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        userService.logout(token);
        return R.ok();
    }

    private String extractToken(String auth) {
        if (auth == null || auth.isBlank()) return null;
        // 支持 "Bearer xxx" 或直接传 token
        if (auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }
}
