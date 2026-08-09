package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Monster;
import com.jjwpp.linxr.entity.PlayerCharacter;
import com.jjwpp.linxr.service.IMonsterService;
import com.jjwpp.linxr.service.IPlayerCharacterService;
import com.jjwpp.linxr.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 图片管理控制器
 * <p>
 * 提供单文件上传和批量导入功能。
 * 批量导入会扫描指定的本地目录，将图片上传到 MinIO 并更新数据库记录。
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private MinioService minioService;

    @Autowired
    private IMonsterService monsterService;

    @Autowired
    private IPlayerCharacterService playerCharacterService;

    /**
     * 上传单个图片文件到 MinIO
     *
     * @param file     图片文件
     * @param category 分类：monsters / classes/male / classes/female
     * @return 完整图片 URL
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(defaultValue = "misc") String category) {
        try {
            String objectName = minioService.uploadFile(file, category);
            String url = minioService.getFileUrl(objectName);
            return R.ok(url);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return R.fail("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入怪物图片
     * 扫描本地 monsters 目录，上传到 MinIO，并更新 monster 表的 image_url 字段
     *
     * @param body 包含 dir 字段（本地图片目录路径）
     * @return 导入结果统计
     */
    @PostMapping("/import/monsters")
    public R<Map<String, Object>> importMonsterImages(@RequestBody Map<String, String> body) {
        String dir = body.get("dir");
        if (dir == null || dir.isBlank()) {
            dir = "../XRLin/src/assets/monsters";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        int success = 0, skip = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        File folder = new File(dir);
        if (!folder.exists() || !folder.isDirectory()) {
            return R.fail("目录不存在: " + dir);
        }

        List<Monster> monsters = monsterService.list();
        Map<String, Monster> monsterMap = new HashMap<>();
        for (Monster m : monsters) {
            monsterMap.put(m.getId(), m);
        }

        File[] files = folder.listFiles((d, name) ->
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            return R.fail("目录中没有图片文件: " + dir);
        }

        for (File file : files) {
            String fileName = file.getName();
            // 文件名去掉扩展名即为 monsterId（如 goblin.jpg -> goblin）
            String monsterId = fileName.substring(0, fileName.lastIndexOf('.'));

            Monster monster = monsterMap.get(monsterId);
            if (monster == null) {
                log.warn("怪物 {} 在数据库中不存在，跳过", monsterId);
                skip++;
                continue;
            }

            try {
                String objectName = "monsters/" + fileName;
                long size = file.length();
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) contentType = "image/jpeg";

                try (FileInputStream fis = new FileInputStream(file)) {
                    minioService.uploadLocalFile(fis, objectName, contentType, size);
                }

                String url = minioService.getFileUrl(objectName);
                monster.setImageUrl(url);
                monsterService.updateById(monster);
                success++;
                log.info("怪物 {} 图片导入成功: {}", monsterId, url);

            } catch (Exception e) {
                fail++;
                errors.add(monsterId + ": " + e.getMessage());
                log.error("怪物 {} 图片导入失败", monsterId, e);
            }
        }

        result.put("total", files.length);
        result.put("success", success);
        result.put("skip", skip);
        result.put("fail", fail);
        result.put("errors", errors);
        return R.ok(result);
    }

    /**
     * 批量导入职业图片
     * 扫描本地 classes 目录，上传男性和女性职业图片到 MinIO
     * 并更新 player_character 表中对应角色的 male_image_url / female_image_url
     *
     * @param body 包含 dir 字段（本地图片目录路径）
     * @return 导入结果统计
     */
    @PostMapping("/import/classes")
    public R<Map<String, Object>> importClassImages(@RequestBody Map<String, String> body) {
        String dir = body.get("dir");
        if (dir == null || dir.isBlank()) {
            dir = "../XRLin/src/assets/classes";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        File folder = new File(dir);
        if (!folder.exists() || !folder.isDirectory()) {
            return R.fail("目录不存在: " + dir);
        }

        File[] files = folder.listFiles((d, name) ->
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            return R.fail("目录中没有图片文件: " + dir);
        }

        for (File file : files) {
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

            try {
                String objectName;
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) contentType = "image/jpeg";

                long size = file.length();

                // 判断男性还是女性图片
                if (baseName.endsWith("_female")) {
                    objectName = "classes/female/" + fileName;
                } else {
                    objectName = "classes/male/" + fileName;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    minioService.uploadLocalFile(fis, objectName, contentType, size);
                }

                String url = minioService.getFileUrl(objectName);
                success++;
                log.info("职业图片导入成功: {} -> {}", fileName, url);

            } catch (Exception e) {
                fail++;
                errors.add(fileName + ": " + e.getMessage());
                log.error("职业图片 {} 导入失败", fileName, e);
            }
        }

        result.put("total", files.length);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return R.ok(result);
    }
}
