package com.yiweilai.wms.stock.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.stock.dto.StockLogQueryDTO;
import com.yiweilai.wms.stock.service.StockLogService;
import com.yiweilai.wms.stock.vo.StockLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存流水 Controller
 */
@RestController
@RequestMapping("/api/stock-logs")
@RequiredArgsConstructor
public class StockLogController {

    private final StockLogService stockLogService;

    /**
     * 库存流水列表（分页）
     */
    @GetMapping
    public Result<PageResult<StockLogVO>> list(StockLogQueryDTO query) {
        return Result.success(stockLogService.findByPage(query));
    }
}
