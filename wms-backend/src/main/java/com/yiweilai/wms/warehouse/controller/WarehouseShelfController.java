package com.yiweilai.wms.warehouse.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.warehouse.dto.WarehouseShelfSaveDTO;
import com.yiweilai.wms.warehouse.service.WarehouseShelfService;
import com.yiweilai.wms.warehouse.vo.WarehouseShelfVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 货架 Controller
 */
@RequirePermission("warehouse.shelf")
@RestController
@RequestMapping("/api/warehouse-shelves")
@RequiredArgsConstructor
public class WarehouseShelfController {

    private final WarehouseShelfService shelfService;

    /**
     * 根据仓库ID查询货架列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<WarehouseShelfVO>> listByWarehouseId(@PathVariable Long warehouseId) {
        return Result.success(shelfService.findByWarehouseId(warehouseId));
    }

    /**
     * 根据ID查询货架详情
     */
    @GetMapping("/{id}")
    public Result<WarehouseShelfVO> getById(@PathVariable Long id) {
        return Result.success(shelfService.getById(id));
    }

    /**
     * 新增货架
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WarehouseShelfSaveDTO dto) {
        return Result.success(shelfService.create(dto));
    }

    /**
     * 修改货架
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WarehouseShelfSaveDTO dto) {
        shelfService.update(dto);
        return Result.success();
    }

    /**
     * 删除货架
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shelfService.delete(id);
        return Result.success();
    }
}
