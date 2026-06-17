package com.yiweilai.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.user.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限校验拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String PERM_CACHE_PREFIX = "cache:user_perm:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 查找注解：方法上的优先，其次类上的
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }

        // 没有注解则放行
        if (annotation == null) {
            return true;
        }

        // 获取当前用户 ID
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            writeForbidden(response, "未登录");
            return false;
        }
        Long userId = Long.parseLong(userIdObj.toString());

        // 超级管理员跳过检查
        Object rolesObj = request.getAttribute("roles");
        if (rolesObj instanceof List<?> roles && roles.contains("SUPER_ADMIN")) {
            return true;
        }

        // 获取用户权限（带缓存）
        String cacheKey = PERM_CACHE_PREFIX + userId;
        List<String> userPerms = cacheService.getOrLoad(cacheKey, 5, TimeUnit.MINUTES,
                () -> permissionService.getPermissionCodesByUserId(userId));

        String requiredPerm = annotation.value();
        if (userPerms == null || !hasPermission(userPerms, requiredPerm)) {
            log.warn("权限不足: userId={}, required={}, has={}", userId, requiredPerm, userPerms);
            writeForbidden(response, "无权限访问该功能");
            return false;
        }

        return true;
    }

    /**
     * 层级权限匹配：
     * - 精确匹配："stock.query" 匹配 "stock.query"
     * - 父级匹配：有 "stock.query" 则自动拥有 "stock" 父权限
     * - 子级匹配：有 "stock" 则自动拥有 "stock.query"、"stock.log" 等子权限
     */
    private boolean hasPermission(List<String> userPerms, String required) {
        if (userPerms.contains(required)) {
            return true;
        }
        for (String perm : userPerms) {
            // 用户有子权限 → 匹配父权限（如 stock.query → stock）
            if (perm.startsWith(required + ".")) {
                return true;
            }
            // 用户有父权限 → 匹配子权限（如 stock → stock.query）
            if (required.startsWith(perm + ".")) {
                return true;
            }
        }
        return false;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
