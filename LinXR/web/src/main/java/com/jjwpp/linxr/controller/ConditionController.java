package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Condition;
import com.jjwpp.linxr.service.IConditionService;
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
@RequestMapping("/api/condition")
public class ConditionController {

    @Autowired
    private IConditionService conditionService;

    @GetMapping("/list")
    public R<List<Condition>> list() {
        return R.ok(conditionService.list());
    }

    @GetMapping("/{id}")
    public R<Condition> getById(@PathVariable String id) {
        return R.ok(conditionService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Condition>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Condition> page = new Page<>(current, size);
        return R.ok(conditionService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Condition condition) {
        return R.ok(conditionService.save(condition));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Condition condition) {
        return R.ok(conditionService.updateById(condition));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(conditionService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(conditionService.count());
    }
}
