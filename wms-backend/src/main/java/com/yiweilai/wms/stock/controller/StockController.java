package com.yiweilai.wms.stock.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.log.annotation.OperationLog;
import com.yiweilai.wms.stock.dto.BatchStockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存 Controller
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * 库存查询（分页）
     */
    @GetMapping("/query")
    public Result<PageResult<StockVO>> query(StockQueryDTO query) {
        return Result.success(stockService.findByPage(query));
    }

    /**
     * 库存调整
     */
    @OperationLog(module = "stock", action = "adjust", targetType = "Stock")
    @PostMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody StockAdjustDTO dto) {
        stockService.adjust(dto);
        return Result.success();
    }

    /**
     * 批量入库
     */
    @OperationLog(module = "stock", action = "batch_adjust", targetType = "Stock")
    @PostMapping("/batch-adjust")
    public Result<Void> batchAdjust(@Valid @RequestBody BatchStockAdjustDTO dto) {
        stockService.batchAdjust(dto);
        return Result.success();
    }

    /**
     * 查询特殊仓库库存（次品仓/报废仓）
     */
    @GetMapping("/special")
    public Result<List<StockVO>> specialStock(
            @RequestParam String type,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String skuName) {
        return Result.success(stockService.findByWarehouseType(type, skuCode, skuName));
    }

    /**
     * 确认可售（次品仓→普通仓）
     */
    @OperationLog(module = "stock", action = "confirm_sellable", targetType = "Stock")
    @PostMapping("/confirm-sellable")
    public Result<Void> confirmSellable(@RequestParam Long stockId, @RequestParam Long targetWarehouseId, @RequestParam Integer quantity) {
        stockService.confirmSellable(stockId, targetWarehouseId, quantity);
        return Result.success();
    }

    /**
     * 确认报废处置（报废仓→移除）
     */
    @OperationLog(module = "stock", action = "confirm_dispose", targetType = "Stock")
    @PostMapping("/confirm-dispose")
    public Result<Void> confirmDispose(@RequestParam Long stockId, @RequestParam Integer quantity) {
        stockService.confirmDispose(stockId, quantity);
        return Result.success();
    }

    /**
     * 确认转入报废仓（次品仓→报废仓）
     */
    @OperationLog(module = "stock", action = "confirm_scrap", targetType = "Stock")
    @PostMapping("/confirm-scrap")
    public Result<Void> confirmScrap(@RequestParam Long stockId, @RequestParam Integer quantity) {
        stockService.confirmScrap(stockId, quantity);
        return Result.success();
    }
}
