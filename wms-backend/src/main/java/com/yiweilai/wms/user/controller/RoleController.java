package com.yiweilai.wms.user.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.service.RoleService;
import com.yiweilai.wms.user.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色 Controller
 */
@Tag(name = "角色管理", description = "角色增删改查、权限配置")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success(roleService.create(dto));
    }

    @Operation(summary = "更新角色")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleSaveDTO dto) {
        roleService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }
}
