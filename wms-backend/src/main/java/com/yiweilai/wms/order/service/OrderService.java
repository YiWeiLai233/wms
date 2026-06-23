package com.yiweilai.wms.order.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.order.dto.OrderImportDTO;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.dto.OrderStatusUpdateDTO;
import com.yiweilai.wms.order.dto.OrderUpdateDTO;
import com.yiweilai.wms.order.vo.OrderVO;

/**
 * 订单 Service
 */
public interface OrderService {

    /**
     * 分页查询订单
     */
    PageResult<OrderVO> findByPage(OrderQueryDTO query);

    /**
     * 根据ID查询订单详情（含明细）
     */
    OrderVO getById(Long id);

    /**
     * 导入订单
     */
    Long importOrder(OrderImportDTO dto);

    /**
     * 更新订单状态
     */
    void updateStatus(OrderStatusUpdateDTO dto);

    /**
     * 更新订单信息
     */
    void update(OrderUpdateDTO dto);

    /**
     * 删除订单（逻辑删除）
     */
    void deleteOrder(Long id);
}
