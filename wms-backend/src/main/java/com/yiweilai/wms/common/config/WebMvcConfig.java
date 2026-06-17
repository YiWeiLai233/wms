package com.yiweilai.wms.common.config;

import com.yiweilai.wms.security.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Value("${image.storage.path:./uploads/images}")
    private String imageStoragePath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/profile",
                        "/api/auth/permissions",
                        "/api/ai/internal/**",
                        "/api/ai/tools/**",
                        "/api/ai/actions/pending",
                        "/images/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        String absolutePath = Paths.get(imageStoragePath).toAbsolutePath().toUri().toString();

        // 映射图片存储目录
        registry.addResourceHandler("/images/**")
                .addResourceLocations(absolutePath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域访问图片
        registry.addMapping("/images/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}
