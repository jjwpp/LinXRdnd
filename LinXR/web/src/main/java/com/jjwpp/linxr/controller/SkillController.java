package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Skill;
import com.jjwpp.linxr.service.ISkillService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lxr
 * @since 2026-07-30
 */
@RestController
@RequestMapping("/api/skill")
public class SkillController {

    @Autowired
    private ISkillService skillService;

    @GetMapping("/list")
    public R<List<Skill>> list() {
        return R.ok(skillService.list());
    }

    @GetMapping("/{id}")
    public R<Skill> getById(@PathVariable String id) {
        return R.ok(skillService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Skill>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Skill> page = new Page<>(current, size);
        return R.ok(skillService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Skill skill) {
        return R.ok(skillService.save(skill));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Skill skill) {
        return R.ok(skillService.updateById(skill));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(skillService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(skillService.count());
    }
}
