package com.yiweilai.wms.user.controller;

import com.yiweilai.wms.common.Constants;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.security.JwtUtils;
import com.yiweilai.wms.user.dto.LoginRequest;
import com.yiweilai.wms.user.dto.LoginResponse;
import com.yiweilai.wms.user.dto.UserSaveDTO;
import com.yiweilai.wms.user.service.UserService;
import com.yiweilai.wms.user.vo.RoleVO;
import com.yiweilai.wms.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户登录、用户信息管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final CacheService cacheService;

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(Constants.TOKEN_PREFIX)) {
            String token = authHeader.substring(Constants.TOKEN_PREFIX.length());
            // 将 Token 加入黑名单，TTL 等于 Token 剩余有效期
            long remaining = jwtUtils.getTokenRemainingTime(token);
            if (remaining > 0) {
                String blacklistKey = JwtUtils.getTokenBlacklistKey(token);
                cacheService.set(blacklistKey, "logout", remaining, TimeUnit.MILLISECONDS);
            }
        }
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/auth/profile")
    public Result<UserVO> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        UserVO user = userService.getCurrentUser(userId);
        return Result.success(user);
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<UserVO> result = userService.listUsers(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/users/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getById(id);
        return Result.success(user);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/users")
    public Result<Long> createUser(@Valid @RequestBody UserSaveDTO dto) {
        Long userId = userService.create(dto);
        return Result.success(userId);
    }

    @Operation(summary = "修改用户")
    @PutMapping("/users")
    public Result<Void> updateUser(@Valid @RequestBody UserSaveDTO dto) {
        userService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public Result<List<RoleVO>> listRoles() {
        List<RoleVO> roles = userService.listRoles();
        return Result.success(roles);
    }
}
