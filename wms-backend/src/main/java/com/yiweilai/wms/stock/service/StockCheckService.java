package com.yiweilai.wms.stock.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockCheckCreateDTO;
import com.yiweilai.wms.stock.dto.StockCheckQueryDTO;
import com.yiweilai.wms.stock.dto.StockCheckSubmitDTO;
import com.yiweilai.wms.stock.vo.StockCheckVO;

/**
 * 库存盘点 Service
 */
public interface StockCheckService {

    /**
     * 创建盘点单
     */
    Long create(StockCheckCreateDTO dto);

    /**
     * 获取盘点单详情
     */
    StockCheckVO getById(Long id);

    /**
     * 提交盘点结果
     */
    void submit(StockCheckSubmitDTO dto);

    /**
     * 分页查询盘点单
     */
    PageResult<StockCheckVO> findByPage(StockCheckQueryDTO query);
}
