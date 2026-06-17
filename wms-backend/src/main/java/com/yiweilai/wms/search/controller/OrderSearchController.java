package com.yiweilai.wms.search.controller;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.search.dto.OrderSearchDTO;
import com.yiweilai.wms.search.service.OrderSearchService;
import com.yiweilai.wms.search.vo.OrderSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 订单搜索 Controller
 */
@RequirePermission("order.list")
@RestController
@RequestMapping("/api/orders/search")
@RequiredArgsConstructor
public class OrderSearchController {

    private final OrderSearchService orderSearchService;

    /**
     * 订单快速搜索
     */
    @GetMapping
    public Result<PageResult<OrderSearchVO>> search(OrderSearchDTO query) {
        return Result.success(orderSearchService.search(query));
    }
}
