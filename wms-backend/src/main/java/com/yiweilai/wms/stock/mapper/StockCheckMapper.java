package com.yiweilai.wms.stock.mapper;

import com.yiweilai.wms.stock.entity.StockCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 盘点单 Mapper
 */
@Mapper
public interface StockCheckMapper {

    /**
     * 根据ID查询
     */
    StockCheck findById(@Param("id") Long id);

    /**
     * 根据盘点单号查询
     */
    StockCheck findByCheckNo(@Param("checkNo") String checkNo);

    /**
     * 新增盘点单
     */
    int insert(StockCheck stockCheck);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status);
}
