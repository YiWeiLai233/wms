package com.yiweilai.wms.stock.mapper;

import com.yiweilai.wms.stock.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存流水 Mapper
 */
@Mapper
public interface StockLogMapper {

    /**
     * 分页查询流水
     */
    List<StockLog> findByPage(@Param("bizType") String bizType,
                              @Param("bizNo") String bizNo,
                              @Param("platformOrderNo") String platformOrderNo,
                              @Param("skuId") Long skuId,
                              @Param("skuCode") String skuCode,
                              @Param("warehouseId") Long warehouseId,
                              @Param("startTime") String startTime,
                              @Param("endTime") String endTime);

    /**
     * 新增流水
     */
    int insert(StockLog stockLog);
}
