package com.jjwpp.linxr.service;

import com.jjwpp.linxr.entity.ClassLevelProgression;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IClassLevelProgressionService extends IService<ClassLevelProgression> {

    /**
     * 查询指定职业和等级的所有升级奖励
     */
    List<ClassLevelProgression> getByClassAndLevel(String classId, int level);
}
