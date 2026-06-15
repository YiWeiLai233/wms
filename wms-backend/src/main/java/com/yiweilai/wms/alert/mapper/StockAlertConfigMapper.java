package com.yiweilai.wms.alert.mapper;

import com.yiweilai.wms.alert.entity.StockAlertConfig;
import com.yiweilai.wms.alert.vo.StockAlertConfigVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存预警配置 Mapper
 */
@Mapper
public interface StockAlertConfigMapper {

    /**
     * 分页查询预警配置（关联SKU和仓库）
     */
    List<StockAlertConfigVO> findByPage(@Param("skuCode") String skuCode,
                                        @Param("skuName") String skuName,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("enabled") Integer enabled);

    /**
     * 根据ID查询
     */
    StockAlertConfig findById(@Param("id") Long id);

    /**
     * 查询SKU+仓库的启用配置（优先仓库专属，其次通用）
     */
    StockAlertConfig findEnabledConfig(@Param("skuId") Long skuId,
                                       @Param("warehouseId") Long warehouseId);

    /**
     * 查询SKU的所有启用配置（用于批量计算）
     */
    List<StockAlertConfig> findEnabledBySkuIds(@Param("skuIds") List<Long> skuIds);

    /**
     * 检查是否存在相同SKU+仓库的配置
     */
    StockAlertConfig findBySkuAndWarehouse(@Param("skuId") Long skuId,
                                           @Param("warehouseId") Long warehouseId);

    /**
     * 新增
     */
    int insert(StockAlertConfig config);

    /**
     * 更新
     */
    int update(StockAlertConfig config);

    /**
     * 逻辑删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新启用状态
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    /**
     * 根据SKU ID查询通用预警配置（warehouse_id IS NULL）
     */
    StockAlertConfig findBySkuId(@Param("skuId") Long skuId);
}
