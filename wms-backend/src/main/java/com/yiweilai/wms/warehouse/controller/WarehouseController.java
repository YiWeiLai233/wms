package com.yiweilai.wms.warehouse.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.warehouse.dto.WarehouseQueryDTO;
import com.yiweilai.wms.warehouse.dto.WarehouseSaveDTO;
import com.yiweilai.wms.warehouse.service.WarehouseService;
import com.yiweilai.wms.warehouse.vo.WarehouseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 仓库 Controller
 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * 仓库列表（分页）
     */
    @GetMapping
    public Result<PageResult<WarehouseVO>> list(WarehouseQueryDTO query) {
        return Result.success(warehouseService.findByPage(query));
    }

    /**
     * 根据ID查询仓库详情
     */
    @GetMapping("/{id}")
    public Result<WarehouseVO> getById(@PathVariable Long id) {
        return Result.success(warehouseService.getById(id));
    }

    /**
     * 新增仓库
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WarehouseSaveDTO dto) {
        return Result.success(warehouseService.create(dto));
    }

    /**
     * 修改仓库
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WarehouseSaveDTO dto) {
        warehouseService.update(dto);
        return Result.success();
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.success();
    }
}
