package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Monster;
import com.jjwpp.linxr.service.IMonsterService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monster")
public class MonsterController {

    @Autowired
    private IMonsterService monsterService;

    @GetMapping("/list")
    public R<List<Monster>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<Monster> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(monsterService.list(wrapper));
        }
        return R.ok(monsterService.list());
    }

    @GetMapping("/random")
    public R<Monster> random() {
        QueryWrapper<Monster> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Monster result = monsterService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<Monster>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<Monster> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(monsterService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<Monster> getById(@PathVariable String id) {
        return R.ok(monsterService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Monster>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Monster> page = new Page<>(current, size);
        return R.ok(monsterService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Monster monster) {
        return R.ok(monsterService.save(monster));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Monster monster) {
        return R.ok(monsterService.updateById(monster));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(monsterService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(monsterService.count());
    }
}
