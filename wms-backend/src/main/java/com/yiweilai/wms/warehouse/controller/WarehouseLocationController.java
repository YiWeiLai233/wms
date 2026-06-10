package com.yiweilai.wms.warehouse.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.warehouse.dto.WarehouseLocationSaveDTO;
import com.yiweilai.wms.warehouse.service.WarehouseLocationService;
import com.yiweilai.wms.warehouse.vo.WarehouseLocationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库位 Controller
 */
@RestController
@RequestMapping("/api/warehouse-locations")
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService locationService;

    /**
     * 根据货架ID查询库位列表
     */
    @GetMapping("/shelf/{shelfId}")
    public Result<List<WarehouseLocationVO>> listByShelfId(@PathVariable Long shelfId) {
        return Result.success(locationService.findByShelfId(shelfId));
    }

    /**
     * 根据ID查询库位详情
     */
    @GetMapping("/{id}")
    public Result<WarehouseLocationVO> getById(@PathVariable Long id) {
        return Result.success(locationService.getById(id));
    }

    /**
     * 新增库位
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WarehouseLocationSaveDTO dto) {
        return Result.success(locationService.create(dto));
    }

    /**
     * 修改库位
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WarehouseLocationSaveDTO dto) {
        locationService.update(dto);
        return Result.success();
    }

    /**
     * 删除库位
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return Result.success();
    }
}
