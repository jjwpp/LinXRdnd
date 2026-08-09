package com.jjwpp.linxr.service.impl;

import com.jjwpp.linxr.entity.ClassLevelProgression;
import com.jjwpp.linxr.mapper.ClassLevelProgressionMapper;
import com.jjwpp.linxr.service.IClassLevelProgressionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassLevelProgressionServiceImpl
        extends ServiceImpl<ClassLevelProgressionMapper, ClassLevelProgression>
        implements IClassLevelProgressionService {

    @Override
    public List<ClassLevelProgression> getByClassAndLevel(String classId, int level) {
        return this.list(new LambdaQueryWrapper<ClassLevelProgression>()
                .eq(ClassLevelProgression::getClassId, classId)
                .eq(ClassLevelProgression::getLevel, level)
                .orderByAsc(ClassLevelProgression::getSortOrder));
    }
}
