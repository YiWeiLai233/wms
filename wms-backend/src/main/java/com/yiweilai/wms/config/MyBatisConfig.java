package com.yiweilai.wms.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 */
@Configuration
@MapperScan("com.yiweilai.wms.**.mapper")
public class MyBatisConfig {
}
