package com.edrs.persistence.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.edrs.persistence.mapper")
public class MyBatisConfig {
}
