package com.yiweilai.wms.warehouse.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.warehouse.dto.WarehouseAreaSaveDTO;
import com.yiweilai.wms.warehouse.service.WarehouseAreaService;
import com.yiweilai.wms.warehouse.vo.WarehouseAreaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 库区 Controller
 */
@RequirePermission("warehouse.list")
@RestController
@RequestMapping("/api/warehouse-areas")
@RequiredArgsConstructor
public class WarehouseAreaController {

    private final WarehouseAreaService areaService;

    /**
     * 根据仓库ID查询库区列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<WarehouseAreaVO>> listByWarehouseId(@PathVariable Long warehouseId) {
        return Result.success(areaService.findByWarehouseId(warehouseId));
    }

    /**
     * 根据ID查询库区详情
     */
    @GetMapping("/{id}")
    public Result<WarehouseAreaVO> getById(@PathVariable Long id) {
        return Result.success(areaService.getById(id));
    }

    /**
     * 新增库区
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WarehouseAreaSaveDTO dto) {
        return Result.success(areaService.create(dto));
    }

    /**
     * 修改库区
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WarehouseAreaSaveDTO dto) {
        areaService.update(dto);
        return Result.success();
    }

    /**
     * 删除库区
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        areaService.delete(id);
        return Result.success();
    }
}
