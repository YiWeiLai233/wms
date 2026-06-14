package com.yiweilai.wms.stock.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.vo.StockVO;

import java.util.List;

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

    /**
     * 按仓库类型查询库存（次品仓/报废仓）
     */
    List<StockVO> findByWarehouseType(String warehouseType, String skuCode, String skuName);

    /**
     * 确认可售（次品仓→普通仓）
     */
    void confirmSellable(Long stockId, Long targetWarehouseId, Integer quantity);

    /**
     * 确认报废（报废仓→移除库存）
     */
    void confirmDispose(Long stockId, Integer quantity);

    /**
     * 确认转入报废仓（次品仓→报废仓）
     */
    void confirmScrap(Long stockId, Integer quantity);
}
