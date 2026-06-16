package com.yiweilai.wms.exchange.mapper;

import com.yiweilai.wms.exchange.entity.ExchangeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 换货单 Mapper
 */
@Mapper
public interface ExchangeOrderMapper {

    List<ExchangeOrder> findByPage(@Param("exchangeNo") String exchangeNo,
                                   @Param("orderNo") String orderNo,
                                   @Param("platformOrderNo") String platformOrderNo,
                                   @Param("status") String status,
                                   @Param("warehouseId") Long warehouseId);

    ExchangeOrder findById(@Param("id") Long id);

    int insert(ExchangeOrder order);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateExpressInfo(@Param("id") Long id,
                          @Param("expressCompanyId") Long expressCompanyId,
                          @Param("trackingNo") String trackingNo,
                          @Param("shippingFee") java.math.BigDecimal shippingFee);
}
