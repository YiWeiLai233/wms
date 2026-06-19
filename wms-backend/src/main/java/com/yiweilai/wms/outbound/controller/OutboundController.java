package com.yiweilai.wms.outbound.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.log.annotation.OperationLog;
import com.yiweilai.wms.outbound.dto.OutboundBatchCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
import com.yiweilai.wms.outbound.dto.OutboundUpdateDTO;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 出库 Controller
 */
@RestController
@RequestMapping("/api/outbound")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;

    /**
     * 出库单列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<OutboundOrderVO>> list(OutboundQueryDTO query) {
        return Result.success(outboundService.findByPage(query));
    }

    /**
     * 根据ID查询出库单详情
     */
    @GetMapping("/{id}")
    public Result<OutboundOrderVO> getById(@PathVariable Long id) {
        return Result.success(outboundService.getById(id));
    }

    /**
     * 创建出库单
     */
    @OperationLog(module = "outbound", action = "create", targetType = "OutboundOrder")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody OutboundCreateDTO dto) {
        return Result.success(outboundService.create(dto));
    }

    /**
     * 批量创建出库单
     */
    @OperationLog(module = "outbound", action = "create_batch", targetType = "OutboundOrder")
    @PostMapping("/create-batch")
    public Result<List<Long>> createBatch(@Valid @RequestBody OutboundBatchCreateDTO dto) {
        return Result.success(outboundService.createBatch(dto));
    }

    /**
     * 扫码核对
     */
    @PostMapping("/scan")
    public Result<Void> scan(@Valid @RequestBody OutboundScanDTO dto) {
        outboundService.scan(dto);
        return Result.success();
    }

    /**
     * 确认出库
     */
    @OperationLog(module = "outbound", action = "confirm", targetType = "OutboundOrder")
    @PostMapping("/confirm")
    public Result<Void> confirm(@Valid @RequestBody OutboundConfirmDTO dto) {
        outboundService.confirm(dto);
        return Result.success();
    }

    /**
     * 更新发货单信息
     */
    @OperationLog(module = "outbound", action = "update", targetType = "OutboundOrder", targetIdParam = "id")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody OutboundUpdateDTO dto) {
        dto.setId(id);
        outboundService.update(dto);
        return Result.success();
    }

    /**
     * 取消出库单
     */
    @OperationLog(module = "outbound", action = "cancel", targetType = "OutboundOrder", targetIdParam = "id")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        outboundService.cancel(id);
        return Result.success();
    }
}
