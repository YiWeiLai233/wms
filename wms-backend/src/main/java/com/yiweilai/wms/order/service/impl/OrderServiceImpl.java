package com.yiweilai.wms.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.order.dto.OrderImportDTO;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.dto.OrderStatusUpdateDTO;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.order.service.OrderService;
import com.yiweilai.wms.order.vo.OrderItemVO;
import com.yiweilai.wms.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;

    @Override
    public PageResult<OrderVO> findByPage(OrderQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<SalesOrder> orders = orderMapper.findByPage(
                query.getOrderNo(), query.getPlatformOrderNo(),
                query.getReceiverName(), query.getReceiverPhone(),
                query.getOrderStatus(), query.getWarehouseId());

        PageInfo<SalesOrder> pageInfo = new PageInfo<>(orders);

        List<OrderVO> voList = orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<OrderVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public OrderVO getById(Long id) {
        SalesOrder order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        OrderVO vo = convertToVO(order);

        // 查询订单明细
        List<OrderItemVO> items = orderItemMapper.findByOrderId(id).stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importOrder(OrderImportDTO dto) {
        // 生成订单号
        String orderNo = "SO" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // 创建订单
        SalesOrder order = new SalesOrder();
        order.setOrderNo(orderNo);
        order.setPlatformOrderNo(dto.getPlatformOrderNo());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark());
        order.setOrderStatus("WAIT_OUTBOUND"); // 默认待出库

        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderImportDTO.OrderItemDTO itemDTO : dto.getItems()) {
            BigDecimal itemTotal = itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        order.setTotalAmount(totalAmount);

        orderMapper.insert(order);

        // 创建订单明细
        for (OrderImportDTO.OrderItemDTO itemDTO : dto.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(itemDTO.getSkuId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setSkuName(itemDTO.getSkuName());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setTotalPrice(itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            orderItemMapper.insert(item);
        }

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(OrderStatusUpdateDTO dto) {
        SalesOrder order = orderMapper.findById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 状态校验
        String currentStatus = order.getOrderStatus();
        String targetStatus = dto.getTargetStatus();

        // 简单状态流转校验
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "不允许从 " + currentStatus + " 变更为 " + targetStatus);
        }

        orderMapper.updateStatus(dto.getOrderId(), targetStatus);

        // 如果是发货状态，更新发货时间
        if ("SHIPPED".equals(targetStatus)) {
            orderMapper.updateShippedAt(dto.getOrderId());
        }
    }

    private boolean isValidTransition(String current, String target) {
        // 定义允许的状态流转
        return switch (current) {
            case "WAIT_PAY" -> "WAIT_OUTBOUND".equals(target) || "CANCELLED".equals(target);
            case "WAIT_OUTBOUND" -> "OUTBOUNDING".equals(target) || "CANCELLED".equals(target);
            case "OUTBOUNDING" -> "SHIPPED".equals(target);
            case "SHIPPED" -> "FINISHED".equals(target) || "RETURNING".equals(target);
            case "RETURNING" -> "RETURNED".equals(target);
            default -> false;
        };
    }

    private OrderVO convertToVO(SalesOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private OrderItemVO convertToItemVO(SalesOrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}
