package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Background;
import com.jjwpp.linxr.service.IBackgroundService;
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
@RequestMapping("/api/background")
public class BackgroundController {

    @Autowired
    private IBackgroundService backgroundService;

    @GetMapping("/list")
    public R<List<Background>> list() {
        return R.ok(backgroundService.list());
    }

    @GetMapping("/{id}")
    public R<Background> getById(@PathVariable String id) {
        return R.ok(backgroundService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Background>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Background> page = new Page<>(current, size);
        return R.ok(backgroundService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Background background) {
        return R.ok(backgroundService.save(background));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Background background) {
        return R.ok(backgroundService.updateById(background));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(backgroundService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(backgroundService.count());
    }
}
