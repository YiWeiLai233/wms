package com.yiweilai.wms.stock.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.stock.dto.StockCheckCreateDTO;
import com.yiweilai.wms.stock.dto.StockCheckQueryDTO;
import com.yiweilai.wms.stock.dto.StockCheckSubmitDTO;
import com.yiweilai.wms.stock.service.StockCheckService;
import com.yiweilai.wms.stock.vo.StockCheckVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存盘点 Controller
 */
@RestController
@RequestMapping("/api/stock-checks")
@RequiredArgsConstructor
public class StockCheckController {

    private final StockCheckService stockCheckService;

    /**
     * 分页查询盘点单
     */
    @GetMapping
    public Result<PageResult<StockCheckVO>> list(StockCheckQueryDTO query) {
        return Result.success(stockCheckService.findByPage(query));
    }

    /**
     * 创建盘点单
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody StockCheckCreateDTO dto) {
        return Result.success(stockCheckService.create(dto));
    }

    /**
     * 获取盘点单详情
     */
    @GetMapping("/{id}")
    public Result<StockCheckVO> getById(@PathVariable Long id) {
        return Result.success(stockCheckService.getById(id));
    }

    /**
     * 提交盘点结果
     */
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody StockCheckSubmitDTO dto) {
        stockCheckService.submit(dto);
        return Result.success();
    }
}
