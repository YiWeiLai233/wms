package com.yiweilai.wms.order.mapper;

import com.yiweilai.wms.order.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface SalesOrderItemMapper {

    /**
     * 根据订单ID查询明细
     */
    List<SalesOrderItem> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 新增明细
     */
    int insert(SalesOrderItem item);

    /**
     * 根据ID删除明细
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据订单ID删除明细
     */
    int deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新明细数量
     */
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
}
