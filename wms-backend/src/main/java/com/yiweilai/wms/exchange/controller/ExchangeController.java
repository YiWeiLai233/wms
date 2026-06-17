package com.yiweilai.wms.exchange.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.exchange.dto.ExchangeCheckDTO;
import com.yiweilai.wms.exchange.dto.ExchangeCreateDTO;
import com.yiweilai.wms.exchange.dto.ExchangeQueryDTO;
import com.yiweilai.wms.exchange.service.ExchangeService;
import com.yiweilai.wms.exchange.vo.ExchangeOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 换货 Controller
 */
@RequirePermission("exchange.list")
@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    /**
     * 换货单列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<ExchangeOrderVO>> list(ExchangeQueryDTO query) {
        return Result.success(exchangeService.findByPage(query));
    }

    /**
     * 根据ID查询换货单详情
     */
    @GetMapping("/{id}")
    public Result<ExchangeOrderVO> getById(@PathVariable Long id) {
        return Result.success(exchangeService.getById(id));
    }

    /**
     * 创建换货单
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody ExchangeCreateDTO dto) {
        return Result.success(exchangeService.create(dto));
    }

    /**
     * 收货（确认收到退回商品）
     */
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        exchangeService.receive(id);
        return Result.success();
    }

    /**
     * 质检（更新退回商品质量状态）
     */
    @PostMapping("/check")
    public Result<Void> check(@Valid @RequestBody ExchangeCheckDTO dto) {
        exchangeService.check(dto);
        return Result.success();
    }

    /**
     * 发货（为换出商品创建出库单）
     */
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long expressCompanyId = null;
        String trackingNo = null;
        BigDecimal shippingFee = null;
        if (body != null) {
            if (body.get("expressCompanyId") != null) {
                expressCompanyId = Long.valueOf(body.get("expressCompanyId").toString());
            }
            trackingNo = (String) body.get("trackingNo");
            if (body.get("shippingFee") != null) {
                shippingFee = new BigDecimal(body.get("shippingFee").toString());
            }
        }
        exchangeService.ship(id, expressCompanyId, trackingNo, shippingFee);
        return Result.success();
    }

    /**
     * 取消换货单
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        exchangeService.cancel(id);
        return Result.success();
    }
}
