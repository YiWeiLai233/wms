package com.yiweilai.wms.search.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.search.dto.OrderSearchDTO;
import com.yiweilai.wms.search.vo.OrderSearchVO;

/**
 * 订单搜索 Service
 */
public interface OrderSearchService {

    /**
     * 订单快速搜索
     */
    PageResult<OrderSearchVO> search(OrderSearchDTO query);
}
