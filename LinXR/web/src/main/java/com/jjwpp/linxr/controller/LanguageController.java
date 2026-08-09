package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Language;
import com.jjwpp.linxr.service.ILanguageService;
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
@RequestMapping("/api/language")
public class LanguageController {

    @Autowired
    private ILanguageService languageService;

    @GetMapping("/list")
    public R<List<Language>> list() {
        return R.ok(languageService.list());
    }

    @GetMapping("/{id}")
    public R<Language> getById(@PathVariable String id) {
        return R.ok(languageService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Language>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Language> page = new Page<>(current, size);
        return R.ok(languageService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Language language) {
        return R.ok(languageService.save(language));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Language language) {
        return R.ok(languageService.updateById(language));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(languageService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(languageService.count());
    }
}
