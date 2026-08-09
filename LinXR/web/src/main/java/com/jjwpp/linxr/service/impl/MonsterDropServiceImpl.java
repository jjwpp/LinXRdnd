package com.jjwpp.linxr.service.impl;

import com.jjwpp.linxr.entity.MonsterDrop;
import com.jjwpp.linxr.mapper.MonsterDropMapper;
import com.jjwpp.linxr.service.IMonsterDropService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonsterDropServiceImpl
        extends ServiceImpl<MonsterDropMapper, MonsterDrop>
        implements IMonsterDropService {

    @Override
    public List<MonsterDrop> getDropsByMonsterId(String monsterId) {
        return this.list(new LambdaQueryWrapper<MonsterDrop>()
                .eq(MonsterDrop::getMonsterId, monsterId));
    }
}
