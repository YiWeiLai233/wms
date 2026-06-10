package com.yiweilai.wms.order.mapper;

import com.yiweilai.wms.order.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface SalesOrderMapper {

    /**
     * 分页查询订单
     */
    List<SalesOrder> findByPage(@Param("orderNo") String orderNo,
                                @Param("platformOrderNo") String platformOrderNo,
                                @Param("receiverName") String receiverName,
                                @Param("receiverPhone") String receiverPhone,
                                @Param("orderStatus") String orderStatus,
                                @Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    SalesOrder findById(@Param("id") Long id);

    /**
     * 根据订单号查询
     */
    SalesOrder findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 新增订单
     */
    int insert(SalesOrder order);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("orderStatus") String orderStatus);

    /**
     * 更新发货时间
     */
    int updateShippedAt(@Param("id") Long id);
}
