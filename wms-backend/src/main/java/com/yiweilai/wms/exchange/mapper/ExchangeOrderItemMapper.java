package com.yiweilai.wms.exchange.mapper;

import com.yiweilai.wms.exchange.entity.ExchangeOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 换货明细 Mapper
 */
@Mapper
public interface ExchangeOrderItemMapper {

    List<ExchangeOrderItem> findByExchangeId(@Param("exchangeId") Long exchangeId);

    ExchangeOrderItem findById(@Param("id") Long id);

    int insert(ExchangeOrderItem item);

    int updateQualityStatus(@Param("id") Long id, @Param("qualityStatus") String qualityStatus);

    /**
     * 查询订单关联的换货换出商品（已完成的换货单）
     */
    List<ExchangeOrderItem> findExchangeItemsByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询订单关联的所有换货明细（退回+换出）
     */
    List<ExchangeOrderItem> findReturnAndExchangeItemsByOrderId(@Param("orderId") Long orderId);
}
