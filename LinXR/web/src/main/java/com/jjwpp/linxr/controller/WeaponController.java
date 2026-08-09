package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Weapon;
import com.jjwpp.linxr.service.IWeaponService;
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
@RequestMapping("/api/weapon")
public class WeaponController {

    @Autowired
    private IWeaponService weaponService;

    @GetMapping("/list")
    public R<List<Weapon>> list() {
        return R.ok(weaponService.list());
    }

    @GetMapping("/{id}")
    public R<Weapon> getById(@PathVariable String id) {
        return R.ok(weaponService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Weapon>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Weapon> page = new Page<>(current, size);
        return R.ok(weaponService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Weapon weapon) {
        return R.ok(weaponService.save(weapon));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Weapon weapon) {
        return R.ok(weaponService.updateById(weapon));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(weaponService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(weaponService.count());
    }
}
