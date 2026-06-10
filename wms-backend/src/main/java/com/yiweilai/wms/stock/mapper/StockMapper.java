package com.yiweilai.wms.stock.mapper;

import com.yiweilai.wms.stock.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存 Mapper
 */
@Mapper
public interface StockMapper {

    /**
     * 分页查询库存
     */
    List<Stock> findByPage(@Param("skuId") Long skuId,
                           @Param("skuCode") String skuCode,
                           @Param("skuName") String skuName,
                           @Param("productName") String productName,
                           @Param("warehouseId") Long warehouseId,
                           @Param("locationId") Long locationId,
                           @Param("locationCode") String locationCode,
                           @Param("stockType") String stockType);

    /**
     * 根据SKU和库位查询库存
     */
    Stock findBySkuAndLocation(@Param("skuId") Long skuId,
                               @Param("locationId") Long locationId);

    /**
     * 根据ID查询
     */
    Stock findById(@Param("id") Long id);

    /**
     * 新增库存
     */
    int insert(Stock stock);

    /**
     * 更新库存数量
     */
    int updateQuantity(@Param("id") Long id,
                       @Param("quantity") Integer quantity);

    /**
     * 扣减库存（防负数）
     */
    int deductQuantity(@Param("id") Long id,
                       @Param("quantity") Integer quantity);

    /**
     * 增加库存
     */
    int addQuantity(@Param("id") Long id,
                    @Param("quantity") Integer quantity);

    /**
     * 增加次品库存
     */
    int addDefectiveQuantity(@Param("id") Long id,
                             @Param("quantity") Integer quantity);

    /**
     * 锁定库存
     */
    int lockQuantity(@Param("id") Long id,
                     @Param("quantity") Integer quantity);

    /**
     * 释放锁定库存
     */
    int releaseQuantity(@Param("id") Long id,
                        @Param("quantity") Integer quantity);
}
