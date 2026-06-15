package com.yiweilai.wms.alert.service;

import com.yiweilai.wms.alert.dto.StockAlertConfigCreateDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigQueryDTO;
import com.yiweilai.wms.alert.dto.StockAlertConfigUpdateDTO;
import com.yiweilai.wms.alert.vo.StockAlertConfigVO;
import com.yiweilai.wms.alert.vo.StockAlertStatisticsVO;
import com.yiweilai.wms.alert.vo.StockAlertStatusVO;
import com.yiweilai.wms.common.PageResult;

import java.util.List;

/**
 * 库存预警配置 Service
 */
public interface StockAlertConfigService {

    /**
     * 分页查询预警配置
     */
    PageResult<StockAlertConfigVO> findByPage(StockAlertConfigQueryDTO query);

    /**
     * 新增预警配置
     */
    Long create(StockAlertConfigCreateDTO dto);

    /**
     * 修改预警配置
     */
    void update(Long id, StockAlertConfigUpdateDTO dto);

    /**
     * 删除预警配置（逻辑删除）
     */
    void delete(Long id);

    /**
     * 启用/禁用预警配置
     */
    void updateEnabled(Long id, Integer enabled);

    /**
     * 查询低库存商品
     */
    List<StockAlertStatusVO> getLowStockList();

    /**
     * 查询缺货商品
     */
    List<StockAlertStatusVO> getOutOfStockList();

    /**
     * 查询预警统计
     */
    StockAlertStatisticsVO getStatistics();

    /**
     * 计算库存预警状态
     * @param skuId SKU ID
     * @param warehouseId 仓库ID
     * @param quantity 当前库存
     * @return 状态：NORMAL/LOW_STOCK/OUT_OF_STOCK
     */
    String calculateStockStatus(Long skuId, Long warehouseId, Integer quantity);

    /**
     * 批量查询SKU的预警配置（用于库存列表联动）
     * @return key=skuId, value=配置（可能为null）
     */
    java.util.Map<Long, com.yiweilai.wms.alert.entity.StockAlertConfig> findConfigMapBySkuIds(List<Long> skuIds);
}
