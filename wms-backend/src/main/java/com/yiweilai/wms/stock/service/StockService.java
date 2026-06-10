package com.yiweilai.wms.stock.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.vo.StockVO;

/**
 * 库存 Service
 */
public interface StockService {

    /**
     * 分页查询库存
     */
    PageResult<StockVO> findByPage(StockQueryDTO query);

    /**
     * 库存调整
     */
    void adjust(StockAdjustDTO dto);
}
