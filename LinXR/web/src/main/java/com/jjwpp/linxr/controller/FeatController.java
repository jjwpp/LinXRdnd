package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Feat;
import com.jjwpp.linxr.service.IFeatService;
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
@RequestMapping("/api/feat")
public class FeatController {

    @Autowired
    private IFeatService featService;

    @GetMapping("/list")
    public R<List<Feat>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<Feat> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(featService.list(wrapper));
        }
        return R.ok(featService.list());
    }

    @GetMapping("/random")
    public R<Feat> random() {
        QueryWrapper<Feat> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Feat result = featService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<Feat>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<Feat> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(featService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<Feat> getById(@PathVariable String id) {
        return R.ok(featService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Feat>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Feat> page = new Page<>(current, size);
        return R.ok(featService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Feat feat) {
        return R.ok(featService.save(feat));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Feat feat) {
        return R.ok(featService.updateById(feat));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(featService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(featService.count());
    }
}
