package com.yiweilai.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.Constants;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.config.CacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${ai.service.token:}")
    private String aiServiceToken;

    /** 不需要认证的路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**",
            "/images/**",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 白名单和跨域预检放行
        if (isWhiteListed(path) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAiServicePath(request)) {
            authenticateAiService(request, response, filterChain);
            return;
        }

        // 获取 Token
        String authHeader = request.getHeader(Constants.TOKEN_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(Constants.TOKEN_PREFIX)) {
            writeUnauthorized(response, "未登录");
            return;
        }

        String token = authHeader.substring(Constants.TOKEN_PREFIX.length());

        // 验证 Token
        if (!jwtUtils.validateToken(token)) {
            writeUnauthorized(response, "登录已过期");
            return;
        }

        // 检查 Token 是否在黑名单中
        String blacklistKey = JwtUtils.getTokenBlacklistKey(token);
        if (cacheService.hasKey(blacklistKey)) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return;
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);
        List<String> roles = jwtUtils.getRolesFromToken(token);

        // 检查用户级登出时间戳（同浏览器多账号场景：logout 后旧 token 全部失效）
        String logoutKey = JwtUtils.getUserLogoutKey(userId);
        Object logoutTime = cacheService.get(logoutKey);
        if (logoutTime != null) {
            long logoutTs = Long.parseLong(logoutTime.toString());
            java.util.Date issuedAt = jwtUtils.getIssuedAtFromToken(token);
            if (issuedAt != null && issuedAt.getTime() < logoutTs) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return;
            }
        }
        if (roles == null) {
            roles = List.of();
        }

        var authorities = roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 设置用户信息到 Request Attribute
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("roles", roles);

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isAiServicePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return pathMatcher.match("/api/ai/internal/**", path)
                || pathMatcher.match("/api/ai/tools/**", path)
                || (HttpMethod.POST.matches(request.getMethod())
                && pathMatcher.match("/api/ai/actions/pending", path));
    }

    private void authenticateAiService(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain) throws IOException, ServletException {
        String token = request.getHeader("X-AI-Service-Token");
        if (!StringUtils.hasText(aiServiceToken) || !aiServiceToken.equals(token)) {
            writeUnauthorized(response, "AI服务认证失败");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "wms-ai-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AI_SERVICE")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("userId", 0L);
        request.setAttribute("username", "wms-ai-service");
        request.setAttribute("roles", List.of("AI_SERVICE"));
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
