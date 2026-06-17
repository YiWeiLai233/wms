package com.yiweilai.wms.alert.controller;

import com.yiweilai.wms.alert.dto.StockAlertConfigCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigQueryDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigUpdateDTO;
import com.yiweilai.wms.alert.service.StockAlertConfigService;
import com.yiweilai.wms.alert.vo.StockAlertConfigVO;
import com.yiweilai.wms.alert.vo.StockAlertStatisticsVO;
import com.yiweilai.wms.alert.vo.StockAlertStatusVO;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.yiweilai.wms.security.RequirePermission;

/**
 * 库存预警配置 Controller
 */
@RequirePermission("system.stock-alert")
@RestController
@RequestMapping("/api/stock-alert-configs")
@RequiredArgsConstructor
public class StockAlertConfigController {

    private final StockAlertConfigService stockAlertConfigService;

    /**
     * 分页查询预警配置
     */
    @GetMapping
    public Result<PageResult<StockAlertConfigVO>> query(StockAlertConfigQueryDTO query) {
        return Result.success(stockAlertConfigService.findByPage(query));
    }

    /**
     * 新增预警配置
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody StockAlertConfigCreateDTO dto) {
        return Result.success(stockAlertConfigService.create(dto));
    }

    /**
     * 修改预警配置
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StockAlertConfigUpdateDTO dto) {
        stockAlertConfigService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除预警配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stockAlertConfigService.delete(id);
        return Result.success();
    }

    /**
     * 启用/禁用预警配置
     */
    @PutMapping("/{id}/enabled")
    public Result<Void> updateEnabled(@PathVariable Long id, @RequestBody StockAlertConfigUpdateDTO dto) {
        stockAlertConfigService.updateEnabled(id, dto.getEnabled());
        return Result.success();
    }

    /**
     * 查询低库存商品
     */
    @GetMapping("/low-stock")
    public Result<List<StockAlertStatusVO>> lowStock() {
        return Result.success(stockAlertConfigService.getLowStockList());
    }

    /**
     * 查询缺货商品
     */
    @GetMapping("/out-of-stock")
    public Result<List<StockAlertStatusVO>> outOfStock() {
        return Result.success(stockAlertConfigService.getOutOfStockList());
    }

    /**
     * 查询预警统计
     */
    @GetMapping("/statistics")
    public Result<StockAlertStatisticsVO> statistics() {
        return Result.success(stockAlertConfigService.getStatistics());
    }
}
