package com.yiweilai.wms.exchange.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exchange.dto.ExchangeCheckDTO;
import com.yiweilai.wms.exchange.dto.ExchangeCreateDTO;
import com.yiweilai.wms.exchange.dto.ExchangeQueryDTO;
import com.yiweilai.wms.exchange.vo.ExchangeOrderVO;

/**
 * 换货 Service
 */
public interface ExchangeService {

    /**
     * 分页查询换货单
     */
    PageResult<ExchangeOrderVO> findByPage(ExchangeQueryDTO query);

    /**
     * 根据ID查询换货单详情（含明细）
     */
    ExchangeOrderVO getById(Long id);

    /**
     * 创建换货单
     */
    Long create(ExchangeCreateDTO dto);

    /**
     * 收货（确认收到退回商品）
     */
    void receive(Long exchangeId);

    /**
     * 质检（更新退回商品的质量状态）
     */
    void check(ExchangeCheckDTO dto);

    /**
     * 发货（为换出商品创建出库单）
     */
    void ship(Long exchangeId, Long expressCompanyId, String trackingNo, java.math.BigDecimal shippingFee);

    /**
     * 取消换货单
     */
    void cancel(Long exchangeId);
}
