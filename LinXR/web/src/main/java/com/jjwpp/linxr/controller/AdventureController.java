package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.dm.DmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 冒险模式接口
 * <p>
 * SSE 端点：
 * - POST /start              开始新冒险（返回 sessionId + 流式开场旁白）
 * - POST /{sessionId}/act    玩家行动（流式旁白回应，战斗/探索自动路由）
 * <p>
 * 普通端点：
 * - POST /{sessionId}/rest              长休（恢复全部 HP + 法术位 + 生命骰）
 * - POST /{sessionId}/short-rest        短休（消耗生命骰恢复部分 HP，邪术师恢复法术位）
 * - POST /{sessionId}/use-item          使用背包物品（药水等消耗品）
 * - POST /{sessionId}/levelup           升级选择（选专长 + 法术）
 * - POST /{sessionId}/encounter/confirm 确认遭遇，进入战斗（ENCOUNTER → COMBAT）
 * - GET  /{sessionId}/character-panel   获取角色面板数据（战斗中可调用）
 * - POST /{sessionId}/end-turn          结束当前回合，执行敌人回合
 */
@RestController
@RequestMapping("/api/adventure")
public class AdventureController {

    @Autowired
    private DmService dmService;

    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter start(@RequestBody Map<String, String> body) {
        SseEmitter emitter = new SseEmitter(180_000L);
        String characterId = body.get("characterId");
        dmService.startAdventure(characterId, emitter);
        return emitter;
    }

    @PostMapping(value = "/{sessionId}/act", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter act(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        SseEmitter emitter = new SseEmitter(180_000L);
        String action = body.get("action");
        if (action == null || action.isBlank()) {
            action = "继续探索";
        }
        dmService.processAction(sessionId, action, emitter);
        return emitter;
    }

    /**
     * 长休：恢复全部 HP 和法术位
     */
    @PostMapping("/{sessionId}/rest")
    public R<Map<String, Object>> rest(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.longRest(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 短休：消耗生命骰恢复部分 HP，邪术师恢复法术位
     */
    @PostMapping("/{sessionId}/short-rest")
    public R<Map<String, Object>> shortRest(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.shortRest(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 使用背包物品（药水等消耗品）
     * Body: { "itemId": "potion-of-healing" }
     */
    @PostMapping("/{sessionId}/use-item")
    public R<Map<String, Object>> useItem(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String itemId = body.get("itemId");
        Map<String, Object> result = dmService.useItem(sessionId, itemId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 获取升级信息（升级预览数据）
     * 返回：角色信息 + 升级奖励列表 + 可选法术/专长 + HP预览
     */
    @GetMapping("/{sessionId}/level-up-info")
    public R<Map<String, Object>> getLevelUpInfo(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.getLevelUpInfo(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 升级选择
     * Body: {
     *   "asi": {"type": "stat", "stat": "str", "amount": 2} | {"type": "feat", "featId": "xxx"},
     *   "spellId": "spell_xxx",
     *   "combatStyle": "defense",
     *   "featId": "feat_xxx"
     * }
     */
    @PostMapping("/{sessionId}/levelup")
    public R<Map<String, Object>> levelUp(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> choices = (Map<String, Object>) body.get("choices");
        if (choices == null) {
            // 兼容旧格式：从 featId/spellId 构建 choices
            choices = new java.util.HashMap<>();
            String featId = (String) body.get("featId");
            String spellId = (String) body.get("spellId");
            if (featId != null) choices.put("featId", featId);
            if (spellId != null) choices.put("spellId", spellId);
        }
        Map<String, Object> result = dmService.applyLevelUp(sessionId, choices);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 确认遭遇：玩家在遭遇弹窗点击"进入战斗"后调用
     * 将游戏状态从 ENCOUNTER 切换到 COMBAT
     */
    @PostMapping("/{sessionId}/encounter/confirm")
    public R<Map<String, Object>> confirmEncounter(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.confirmEncounter(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 获取角色面板数据（战斗中可调用，不退出战斗）
     * 返回完整角色信息：六维属性、战斗属性、法术、装备等
     */
    @GetMapping("/{sessionId}/character-panel")
    public R<Map<String, Object>> getCharacterPanel(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.getCharacterPanel(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }

    /**
     * 结束回合：玩家主动结束当前回合，触发敌人回合执行
     */
    @PostMapping("/{sessionId}/end-turn")
    public R<Map<String, Object>> endTurn(@PathVariable String sessionId) {
        Map<String, Object> result = dmService.endTurn(sessionId);
        if (result.containsKey("error")) {
            return R.fail((String) result.get("error"));
        }
        return R.ok(result);
    }
}
