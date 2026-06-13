package com.yiweilai.wms.returns.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;

/**
 * 退货 Service
 */
public interface ReturnService {

    /**
     * 分页查询退货单
     */
    PageResult<ReturnOrderVO> findByPage(ReturnQueryDTO query);

    /**
     * 根据ID查询退货单详情
     */
    ReturnOrderVO getById(Long id);

    /**
     * 创建退货单
     */
    Long create(ReturnCreateDTO dto);

    /**
     * 退货质检
     */
    void check(ReturnCheckDTO dto);

    /**
     * 确认退货入库
     */
    void confirm(Long returnId);

    /**
     * 取消退货单
     */
    void cancel(Long returnId);

    /**
     * 按订单ID取消退货单
     */
    void cancelByOrderId(Long orderId);
}
