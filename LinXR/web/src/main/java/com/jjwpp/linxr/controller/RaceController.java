package com.jjwpp.linxr.controller;

import com.jjwpp.linxr.common.base.R;
import com.jjwpp.linxr.entity.Race;
import com.jjwpp.linxr.service.IRaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/race")
public class RaceController {

    @Autowired
    private IRaceService raceService;

    @GetMapping("/list")
    public R<List<Race>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            QueryWrapper<Race> wrapper = new QueryWrapper<>();
            wrapper.like("name", q).or().like("summary", q);
            return R.ok(raceService.list(wrapper));
        }
        return R.ok(raceService.list());
    }

    @GetMapping("/random")
    public R<Race> random() {
        QueryWrapper<Race> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT 1");
        Race result = raceService.getOne(wrapper);
        return result != null ? R.ok(result) : R.fail("No data");
    }

    @GetMapping("/random/some")
    public R<List<Race>> randomSome(@RequestParam(defaultValue = "3") int count) {
        QueryWrapper<Race> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + Math.min(count, 100));
        return R.ok(raceService.list(wrapper));
    }

    @GetMapping("/{id}")
    public R<Race> getById(@PathVariable String id) {
        return R.ok(raceService.getById(id));
    }

    @GetMapping("/page")
    public R<Page<Race>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<Race> page = new Page<>(current, size);
        return R.ok(raceService.page(page));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody Race race) {
        return R.ok(raceService.save(race));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody Race race) {
        return R.ok(raceService.updateById(race));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(raceService.removeById(id));
    }

    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(raceService.count());
    }
}
