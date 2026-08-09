package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Spell;
import com.jjwpp.linxr.service.ISpellService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spell")
public class SpellController {

    @Autowired
    private ISpellService spellService;

    @GetMapping("/list")
    public R<List<Spell>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<Spell> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(spellService.list(wrapper));
        }
        return R.ok(spellService.list());
    }

    @GetMapping("/random")
    public R<Spell> random() {
        QueryWrapper<Spell> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Spell result = spellService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<Spell>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<Spell> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(spellService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<Spell> getById(@PathVariable String id) {
        return R.ok(spellService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Spell>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Spell> page = new Page<>(current, size);
        return R.ok(spellService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Spell spell) {
        return R.ok(spellService.save(spell));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Spell spell) {
        return R.ok(spellService.updateById(spell));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(spellService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(spellService.count());
    }
}
