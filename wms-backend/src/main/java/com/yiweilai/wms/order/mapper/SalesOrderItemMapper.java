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
     * 根据订单ID删除明细
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
}
