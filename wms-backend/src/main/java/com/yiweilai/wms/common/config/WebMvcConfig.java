package com.yiweilai.wms.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${image.storage.path:./uploads/images}")
    private String imageStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射图片存储目录
        // 确保路径以/结尾
        String location = imageStoragePath;
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location = location + "/";
        }
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域访问图片
        registry.addMapping("/images/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}
