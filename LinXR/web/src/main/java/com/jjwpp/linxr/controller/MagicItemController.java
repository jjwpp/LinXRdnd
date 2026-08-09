package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.MagicItem;
import com.jjwpp.linxr.service.IMagicItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
@RequestMapping("/api/magic-item")
public class MagicItemController {

    @Autowired
    private IMagicItemService magicItemService;

    @GetMapping("/list")
    public R<List<MagicItem>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<MagicItem> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(magicItemService.list(wrapper));
        }
        return R.ok(magicItemService.list());
    }

    @GetMapping("/random")
    public R<MagicItem> random() {
        QueryWrapper<MagicItem> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        MagicItem result = magicItemService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<MagicItem>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<MagicItem> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(magicItemService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<MagicItem> getById(@PathVariable String id) {
        return R.ok(magicItemService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<MagicItem>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<MagicItem> page = new Page<>(current, size);
        return R.ok(magicItemService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody MagicItem magicItem) {
        return R.ok(magicItemService.save(magicItem));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody MagicItem magicItem) {
        return R.ok(magicItemService.updateById(magicItem));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(magicItemService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(magicItemService.count());
    }
}
