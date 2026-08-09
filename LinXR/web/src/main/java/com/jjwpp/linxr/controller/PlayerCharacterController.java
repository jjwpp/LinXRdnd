package com.jjwpp.linxr.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.PlayerCharacter;
import com.jjwpp.linxr.entity.User;
import com.jjwpp.linxr.common.config.MinioConfig;
import com.jjwpp.linxr.service.IPlayerCharacterService;
import com.jjwpp.linxr.service.IUserService;
import com.jjwpp.linxr.service.ICharacterInventoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/player-character")
public class PlayerCharacterController {

    @Autowired
    private IPlayerCharacterService playerCharacterService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICharacterInventoryService inventoryService;

    @Autowired
    private MinioConfig minioConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据职业ID构建男女角色立绘 MinIO URL
     */
    private void setClassImageUrls(PlayerCharacter pc, String classId) {
        if (classId == null || classId.isBlank()) return;
        String base = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/classes";
        pc.setMaleImageUrl(base + "/male/" + classId + ".jpg");
        pc.setFemaleImageUrl(base + "/female/" + classId + "_female.jpg");
    }

    /**
     * 角色列表 — 按当前登录用户过滤
     */
    @GetMapping("/list")
    public R<List<PlayerCharacter>> list(
            @RequestParam(required = false) String q,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);
        QueryWrapper<PlayerCharacter> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId());

        if (q != null && !q.isBlank()) {
            wrapper.and(w -> w.like("name", q).or().like("player_name", q));
        }
        wrapper.orderByDesc("created_at");
        return R.ok(playerCharacterService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<PlayerCharacter> getById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        User user = resolveUser(auth);
        PlayerCharacter pc = playerCharacterService.getById(id);
        if (pc == null) {
            return R.fail("角色不存在");
        }
        // 校验归属：只有角色拥有者才能查看
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权访问该角色");
        }
        return R.ok(pc);
    }

    /**
     * 创建角色 — 自动绑定当前登录用户
     * <p>
     * userId 和 playerName 由后端从 token 自动填充，前端无需传
     */
    @PostMapping
    public R<Boolean> save(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        User user = resolveUser(auth);

        PlayerCharacter pc = new PlayerCharacter();
        pc.setName((String) body.get("name"));
        pc.setPlayerName(user.getNickname());   // 玩家名 = 用户昵称
        pc.setUserId(user.getId());              // 绑定用户ID
        pc.setLevel(body.get("level") != null ? ((Number) body.get("level")).intValue() : 1);
        pc.setRaceId((String) body.get("raceId"));
        pc.setClassId((String) body.get("classId"));
        pc.setGender(body.get("gender") != null ? (String) body.get("gender") : "male");
        pc.setSubtitle((String) body.get("subtitle"));
        pc.setSummary((String) body.get("summary"));

        // 根据职业自动设置男女角色立绘 URL（MinIO）
        setClassImageUrls(pc, pc.getClassId());

        // 六维属性
        pc.setStrength(body.get("strength") != null ? ((Number) body.get("strength")).intValue() : 10);
        pc.setDexterity(body.get("dexterity") != null ? ((Number) body.get("dexterity")).intValue() : 10);
        pc.setConstitution(body.get("constitution") != null ? ((Number) body.get("constitution")).intValue() : 10);
        pc.setIntelligence(body.get("intelligence") != null ? ((Number) body.get("intelligence")).intValue() : 10);
        pc.setWisdom(body.get("wisdom") != null ? ((Number) body.get("wisdom")).intValue() : 10);
        pc.setCharisma(body.get("charisma") != null ? ((Number) body.get("charisma")).intValue() : 10);

        // Convert arrays to JSON strings
        try {
            if (body.get("featIds") != null) {
                pc.setFeatIds(objectMapper.writeValueAsString(body.get("featIds")));
            }
            if (body.get("spellIds") != null) {
                pc.setSpellIds(objectMapper.writeValueAsString(body.get("spellIds")));
            }
        } catch (JsonProcessingException e) {
            return R.fail("JSON serialize error: " + e.getMessage());
        }

        boolean saved = playerCharacterService.save(pc);
        if (saved) {
            initStartingInventory(pc.getId(), pc.getClassId());
        }
        return R.ok(saved);
    }

    @PutMapping
    public R<Boolean> update(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String id = (String) body.get("id");
        if (id == null || id.isBlank()) {
            return R.fail("id is required");
        }

        User user = resolveUser(auth);
        PlayerCharacter pc = playerCharacterService.getById(id);
        if (pc == null) {
            return R.fail("Character not found");
        }
        // 校验归属
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权修改该角色");
        }

        if (body.get("name") != null) pc.setName((String) body.get("name"));
        // playerName 和 userId 不可手动修改
        if (body.get("level") != null) pc.setLevel(((Number) body.get("level")).intValue());
        if (body.get("raceId") != null) pc.setRaceId((String) body.get("raceId"));
        if (body.get("classId") != null) {
            pc.setClassId((String) body.get("classId"));
            // 职业变更时更新立绘 URL
            setClassImageUrls(pc, pc.getClassId());
        }
        if (body.get("gender") != null) pc.setGender((String) body.get("gender"));
        if (body.get("subtitle") != null) pc.setSubtitle((String) body.get("subtitle"));
        if (body.get("summary") != null) pc.setSummary((String) body.get("summary"));

        // 六维属性
        if (body.get("strength") != null) pc.setStrength(((Number) body.get("strength")).intValue());
        if (body.get("dexterity") != null) pc.setDexterity(((Number) body.get("dexterity")).intValue());
        if (body.get("constitution") != null) pc.setConstitution(((Number) body.get("constitution")).intValue());
        if (body.get("intelligence") != null) pc.setIntelligence(((Number) body.get("intelligence")).intValue());
        if (body.get("wisdom") != null) pc.setWisdom(((Number) body.get("wisdom")).intValue());
        if (body.get("charisma") != null) pc.setCharisma(((Number) body.get("charisma")).intValue());

        try {
            if (body.get("featIds") != null) {
                pc.setFeatIds(objectMapper.writeValueAsString(body.get("featIds")));
            }
            if (body.get("spellIds") != null) {
                pc.setSpellIds(objectMapper.writeValueAsString(body.get("spellIds")));
            }
        } catch (JsonProcessingException e) {
            return R.fail("JSON serialize error: " + e.getMessage());
        }
        return R.ok(playerCharacterService.updateById(pc));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        User user = resolveUser(auth);
        PlayerCharacter pc = playerCharacterService.getById(id);
        if (pc == null) {
            return R.fail("角色不存在");
        }
        // 校验归属：只有拥有者才能删除
        if (pc.getUserId() != null && !pc.getUserId().equals(user.getId())) {
            return R.fail("无权删除该角色");
        }
        return R.ok(playerCharacterService.removeById(id));
    }

    // ═══ 内部方法 ═══

    /** 生命药水 item_id */
    private static final String POTION_HEALING = "item_potion_healing";
    private static final int STARTING_POTION_COUNT = 2;

    /**
     * 职业初始装备映射 — classId → {weaponId, armorId}
     * armorId 为 null 表示该职业不穿护甲（依赖天生防御/无甲防御）
     */
    private static final Map<String, String[]> STARTING_EQUIPMENT = Map.ofEntries(
            Map.entry("barbarian",  new String[]{"item_weapon_handaxe",     "item_armor_leather"}),
            Map.entry("bard",       new String[]{"item_weapon_rapier",      "item_armor_leather"}),
            Map.entry("cleric",     new String[]{"item_weapon_mace",        "item_armor_chain"}),
            Map.entry("druid",      new String[]{"item_weapon_scimitar",    "item_armor_leather"}),
            Map.entry("fighter",    new String[]{"item_weapon_longsword",   "item_armor_chain"}),
            Map.entry("monk",       new String[]{"item_weapon_shortsword",  null}),
            Map.entry("paladin",    new String[]{"item_weapon_longsword",   "item_armor_chain"}),
            Map.entry("ranger",     new String[]{"item_weapon_shortsword",  "item_armor_leather"}),
            Map.entry("rogue",      new String[]{"item_weapon_shortsword",  "item_armor_leather"}),
            Map.entry("sorcerer",   new String[]{"item_weapon_dagger",      null}),
            Map.entry("warlock",    new String[]{"item_weapon_dagger",      "item_armor_leather"}),
            Map.entry("wizard",     new String[]{"item_weapon_dagger",      null})
    );

    /**
     * 初始化角色起始背包：
     *   1. 2 瓶生命药水
     *   2. 按职业发放基础武器（已装备）
     *   3. 按职业发放基础防具（已装备，部分职业无甲）
     */
    private void initStartingInventory(String characterId, String classId) {
        // 1. 生命药水 ×2
        try {
            inventoryService.addItemToInventory(characterId, POTION_HEALING, STARTING_POTION_COUNT);
        } catch (Exception e) {
            // 药水发放失败不阻断流程，记录日志即可
            System.err.println("[StartingEquipment] 添加生命药水失败: " + e.getMessage());
        }

        // 2. 职业初始武器 + 防具
        String[] equip = STARTING_EQUIPMENT.get(classId);
        if (equip == null) {
            // 未知职业：给一把短剑兜底
            equip = new String[]{"item_weapon_shortsword", null};
        }

        String weaponId = equip[0];
        String armorId = equip[1];

        try {
            inventoryService.addEquippedItem(characterId, weaponId, "WEAPON");
        } catch (Exception e) {
            System.err.println("[StartingEquipment] 添加初始武器失败(" + weaponId + "): " + e.getMessage());
        }

        if (armorId != null) {
            try {
                inventoryService.addEquippedItem(characterId, armorId, "ARMOR");
            } catch (Exception e) {
                System.err.println("[StartingEquipment] 添加初始防具失败(" + armorId + "): " + e.getMessage());
            }
        }
    }

    /**
     * 从 Authorization 头解析当前登录用户，未登录则抛异常
     */
    private User resolveUser(String auth) {
        String token = extractToken(auth);
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }

    private String extractToken(String auth) {
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }
}
