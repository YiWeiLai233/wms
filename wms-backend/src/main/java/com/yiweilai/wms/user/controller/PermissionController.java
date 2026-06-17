package com.yiweilai.wms.user.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.user.service.PermissionService;
import com.yiweilai.wms.user.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限 Controller
 */
@Tag(name = "权限管理", description = "权限列表、角色权限配置")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取权限树")
    @GetMapping("/permissions/tree")
    public Result<List<PermissionVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @Operation(summary = "获取所有权限（扁平）")
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> getAllPermissions() {
        return Result.success(permissionService.getAllPermissions());
    }

    @Operation(summary = "获取角色的权限ID列表")
    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(permissionService.getPermissionIdsByRoleId(roleId));
    }

    @Operation(summary = "获取当前用户的权限编码列表")
    @GetMapping("/auth/permissions")
    public Result<List<String>> getCurrentUserPermissions(@RequestAttribute("userId") Long userId) {
        return Result.success(permissionService.getPermissionCodesByUserId(userId));
    }
}
