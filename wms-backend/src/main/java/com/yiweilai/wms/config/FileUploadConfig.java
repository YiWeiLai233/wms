package com.yiweilai.wms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileUploadConfig {

    /** 上传目录 */
    private String uploadDir = "./uploads";
}
