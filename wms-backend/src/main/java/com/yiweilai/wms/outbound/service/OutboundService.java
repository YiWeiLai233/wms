package com.yiweilai.wms.outbound.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;

/**
 * 出库 Service
 */
public interface OutboundService {

    /**
     * 分页查询出库单
     */
    PageResult<OutboundOrderVO> findByPage(OutboundQueryDTO query);

    /**
     * 根据ID查询出库单详情
     */
    OutboundOrderVO getById(Long id);

    /**
     * 创建出库单
     */
    Long create(OutboundCreateDTO dto);

    /**
     * 扫码核对
     */
    void scan(OutboundScanDTO dto);

    /**
     * 确认出库（扣减库存）
     */
    void confirm(OutboundConfirmDTO dto);
}
