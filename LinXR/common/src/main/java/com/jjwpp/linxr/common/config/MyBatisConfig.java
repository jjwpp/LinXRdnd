package com.jjwpp.linxr.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.jjwpp.linxr.mapper")
public class MyBatisConfig {
}
