package com.yiweilai.wms.returns.mapper;

import com.yiweilai.wms.returns.entity.ReturnOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退货单 Mapper
 */
@Mapper
public interface ReturnOrderMapper {

    /**
     * 分页查询退货单
     */
    List<ReturnOrder> findByPage(@Param("returnNo") String returnNo,
                                 @Param("orderNo") String orderNo,
                                 @Param("platformOrderNo") String platformOrderNo,
                                 @Param("status") String status,
                                 @Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    ReturnOrder findById(@Param("id") Long id);

    /**
     * 新增退货单
     */
    int insert(ReturnOrder order);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);

    /**
     * 根据订单ID查询最新的退货单
     */
    ReturnOrder findLatestByOrderId(@Param("orderId") Long orderId);
}
