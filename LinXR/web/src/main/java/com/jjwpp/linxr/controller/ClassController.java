package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Class;
import com.jjwpp.linxr.service.IClassService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class")
public class ClassController {

    @Autowired
    private IClassService classService;

    @GetMapping("/list")
    public R<List<Class>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<Class> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(classService.list(wrapper));
        }
        return R.ok(classService.list());
    }

    @GetMapping("/random")
    public R<Class> random() {
        QueryWrapper<Class> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Class result = classService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<Class>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<Class> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(classService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<Class> getById(@PathVariable String id) {
        return R.ok(classService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Class>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Class> page = new Page<>(current, size);
        return R.ok(classService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Class clazz) {
        return R.ok(classService.save(clazz));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Class clazz) {
        return R.ok(classService.updateById(clazz));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(classService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(classService.count());
    }
}
