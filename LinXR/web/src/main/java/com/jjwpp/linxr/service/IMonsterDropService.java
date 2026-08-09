package com.jjwpp.linxr.service;

import com.jjwpp.linxr.entity.MonsterDrop;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IMonsterDropService extends IService<MonsterDrop> {

    /**
     * 根据怪物ID获取掉落配置列表
     */
    List<MonsterDrop> getDropsByMonsterId(String monsterId);
}
