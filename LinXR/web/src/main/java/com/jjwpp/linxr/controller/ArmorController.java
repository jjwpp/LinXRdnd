package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Armor;
import com.jjwpp.linxr.service.IArmorService;
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
@RequestMapping("/api/armor")
public class ArmorController {

    @Autowired
    private IArmorService armorService;

    @GetMapping("/list")
    public R<List<Armor>> list() {
        return R.ok(armorService.list());
    }

    @GetMapping("/{id}")
    public R<Armor> getById(@PathVariable String id) {
        return R.ok(armorService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Armor>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Armor> page = new Page<>(current, size);
        return R.ok(armorService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Armor armor) {
        return R.ok(armorService.save(armor));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Armor armor) {
        return R.ok(armorService.updateById(armor));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(armorService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(armorService.count());
    }
}
