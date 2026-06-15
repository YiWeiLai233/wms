package com.yiweilai.wms.outbound.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.outbound.dto.OutboundBatchCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
import com.yiweilai.wms.outbound.dto.OutboundUpdateDTO;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;

import java.util.List;

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
     * 批量创建出库单
     */
    List<Long> createBatch(OutboundBatchCreateDTO dto);

    /**
     * 扫码核对
     */
    void scan(OutboundScanDTO dto);

    /**
     * 确认出库（扣减库存）
     */
    void confirm(OutboundConfirmDTO dto);

    /**
     * 更新发货单信息
     */
    void update(OutboundUpdateDTO dto);

    /**
     * 取消出库单
     */
    void cancel(Long id);
}
