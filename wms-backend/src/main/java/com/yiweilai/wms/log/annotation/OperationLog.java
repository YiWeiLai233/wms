package com.yiweilai.wms.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，由 AOP 切面自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 模块名（如 order、product、stock）
     */
    String module();

    /**
     * 操作名（如 create、update、delete、import）
     */
    String action();

    /**
     * 操作对象类型（如 SalesOrder、Product）
     */
    String targetType() default "";

    /**
     * 方法参数中 targetId 的参数名（支持从 @PathVariable 或 @RequestParam 中提取）
     */
    String targetIdParam() default "";
}
