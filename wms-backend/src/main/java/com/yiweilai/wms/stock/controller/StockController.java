package com.yiweilai.wms.stock.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody StockAdjustDTO dto) {
        stockService.adjust(dto);
        return Result.success();
    }
}
