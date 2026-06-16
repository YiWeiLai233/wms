package com.yiweilai.wms.outbound.mapper;

import com.yiweilai.wms.outbound.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出库单 Mapper
 */
@Mapper
public interface OutboundOrderMapper {

    /**
     * 分页查询出库单
     */
    List<OutboundOrder> findByPage(@Param("outboundNo") String outboundNo,
                                   @Param("orderNo") String orderNo,
                                   @Param("platformOrderNo") String platformOrderNo,
                                   @Param("trackingNo") String trackingNo,
                                   @Param("status") String status,
                                   @Param("warehouseId") Long warehouseId);

    /**
     * 根据ID查询
     */
    OutboundOrder findById(@Param("id") Long id);

    /**
     * 根据订单ID查询
     */
    OutboundOrder findByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID和状态查询
     */
    OutboundOrder findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") String status);

    /**
     * 新增出库单
     */
    int insert(OutboundOrder order);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);

    /**
     * 更新拣货人
     */
    int updatePicker(@Param("id") Long id,
                     @Param("pickerId") Long pickerId,
                     @Param("pickerName") String pickerName);

    /**
     * 更新发货时间
     */
    int updateShippedAt(@Param("id") Long id);

    /**
     * 更新快递信息
     */
    int updateExpressInfo(@Param("id") Long id,
                          @Param("expressCompanyId") Long expressCompanyId,
                          @Param("trackingNo") String trackingNo,
                          @Param("shippingFee") BigDecimal shippingFee);

    /**
     * 更新备注
     */
    int updateRemark(@Param("id") Long id,
                     @Param("remark") String remark);
}
