package com.yiweilai.wms.stock.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockLogQueryDTO;
import com.yiweilai.wms.stock.vo.StockLogVO;

/**
 * 库存流水 Service
 */
public interface StockLogService {

    /**
     * 分页查询库存流水
     */
    PageResult<StockLogVO> findByPage(StockLogQueryDTO query);
}
