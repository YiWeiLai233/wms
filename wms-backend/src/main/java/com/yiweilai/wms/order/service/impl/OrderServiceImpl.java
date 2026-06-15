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
import com.yiweilai.wms.privacy.crypto.PrivacyCryptoService;
import com.yiweilai.wms.privacy.crypto.PrivacyHashService;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
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
    private final ProductSkuMapper productSkuMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final PrivacyCryptoService privacyCryptoService;
    private final PrivacyHashService privacyHashService;

    @Override
    public PageResult<OrderVO> findByPage(OrderQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        String receiverNameHash = privacyHashService.hmacSha256(
                privacyHashService.normalizeName(query.getReceiverName()));
        String receiverPhoneHash = privacyHashService.hmacSha256(
                privacyHashService.normalizePhone(query.getReceiverPhone()));
        List<SalesOrder> orders = orderMapper.findByPage(
                query.getOrderNo(), query.getPlatformOrderNo(),
                receiverNameHash, receiverPhoneHash,
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
        order.setPlatformId(dto.getPlatformId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        protectReceiverFields(order);
        order.setRemark(dto.getRemark());
        order.setOrderStatus("WAIT_OUTBOUND"); // 默认待出库

        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderImportDTO.OrderItemDTO itemDTO : dto.getItems()) {
            normalizeOrderItem(itemDTO);
            BigDecimal itemTotal = itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        order.setTotalAmount(totalAmount);

        orderMapper.insert(order);

        // 创建订单明细
        boolean isBrushOrder = dto.getRemark() != null && dto.getRemark().contains("刷单");
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

            // 刷单订单不扣减真实库存
            if (!isBrushOrder) {
                deductStock(itemDTO.getSkuId(), itemDTO.getQuantity(), orderNo, dto.getWarehouseId());
            }
        }

        return order.getId();
    }

    private void normalizeOrderItem(OrderImportDTO.OrderItemDTO itemDTO) {
        ProductSku sku = null;
        if (itemDTO.getSkuId() != null) {
            sku = productSkuMapper.findById(itemDTO.getSkuId());
        }
        if (sku == null && itemDTO.getSkuCode() != null && !itemDTO.getSkuCode().isBlank()) {
            sku = productSkuMapper.findBySkuCode(itemDTO.getSkuCode().trim());
        }
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND, "未找到对应的SKU: " + itemDTO.getSkuCode());
        }

        itemDTO.setSkuId(sku.getId());
        itemDTO.setSkuCode(sku.getSkuCode());
        itemDTO.setSkuName(sku.getName());
        if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
            itemDTO.setQuantity(1);
        }
        if (itemDTO.getUnitPrice() == null) {
            itemDTO.setUnitPrice(BigDecimal.ZERO);
        }
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

        // 如果取消订单，恢复库存
        if ("CANCELLED".equals(targetStatus)) {
            restoreStockForOrder(order);
        }
    }

    /**
     * 取消订单时恢复库存
     */
    private void restoreStockForOrder(SalesOrder order) {
        List<SalesOrderItem> items = orderItemMapper.findByOrderId(order.getId());
        for (SalesOrderItem item : items) {
            Stock stock = stockMapper.findBySkuAndWarehouse(item.getSkuId(), order.getWarehouseId());
            int beforeQty;
            if (stock != null) {
                beforeQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
                stockMapper.addQuantity(stock.getId(), item.getQuantity());
            } else {
                beforeQty = 0;
            }

            StockLog log = new StockLog();
            log.setBizType("RETURN");
            log.setBizNo(order.getOrderNo());
            log.setSkuId(item.getSkuId());
            log.setWarehouseId(order.getWarehouseId());
            log.setQuantityBefore(beforeQty);
            log.setQuantityChange(item.getQuantity());
            log.setQuantityAfter(beforeQty + item.getQuantity());
            log.setRemark("取消订单恢复库存");
            stockLogMapper.insert(log);
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

    /**
     * 扣减库存（每次扣减前重新查询，避免同SKU多次扣减时数据过期）
     */
    private void deductStock(Long skuId, int quantity, String orderNo, Long warehouseId) {
        int remaining = quantity;
        while (remaining > 0) {
            List<Stock> availableStocks = stockMapper.findAvailableBySkuAndWarehouse(skuId, warehouseId);
            if (availableStocks.isEmpty()) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "SKU[" + skuId + "]库存不足，剩余需扣: " + remaining);
            }

            boolean deducted = false;
            for (Stock stock : availableStocks) {
                int beforeQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
                int deductQty = Math.min(beforeQty, remaining);
                if (deductQty <= 0) continue;

                int affected = stockMapper.deductQuantity(stock.getId(), deductQty);
                if (affected == 0) continue;

                StockLog log = new StockLog();
                log.setBizType("OUTBOUND");
                log.setBizNo(orderNo);
                log.setSkuId(skuId);
                log.setWarehouseId(warehouseId);
                log.setQuantityBefore(beforeQty);
                log.setQuantityChange(-deductQty);
                log.setQuantityAfter(beforeQty - deductQty);
                log.setRemark("订单导入扣减库存");
                stockLogMapper.insert(log);

                remaining -= deductQty;
                deducted = true;
                break; // 扣成功了就跳出 for 循环，重新查询最新库存
            }

            if (!deducted) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "SKU[" + skuId + "]库存不足，剩余需扣: " + remaining);
            }
        }
    }

    private OrderVO convertToVO(SalesOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setReceiverName(privacyCryptoService.decrypt(order.getReceiverName()));
        vo.setReceiverPhone(privacyCryptoService.decrypt(order.getReceiverPhone()));
        vo.setReceiverAddress(privacyCryptoService.decrypt(order.getReceiverAddress()));
        return vo;
    }

    private void protectReceiverFields(SalesOrder order) {
        String receiverName = privacyCryptoService.decrypt(order.getReceiverName());
        String receiverPhone = privacyCryptoService.decrypt(order.getReceiverPhone());
        String receiverAddress = privacyCryptoService.decrypt(order.getReceiverAddress());

        order.setReceiverNameHash(privacyHashService.hmacSha256(privacyHashService.normalizeName(receiverName)));
        order.setReceiverPhoneHash(privacyHashService.hmacSha256(privacyHashService.normalizePhone(receiverPhone)));
        order.setReceiverName(privacyCryptoService.encrypt(receiverName));
        order.setReceiverPhone(privacyCryptoService.encrypt(receiverPhone));
        order.setReceiverAddress(privacyCryptoService.encrypt(receiverAddress));
    }

    private OrderItemVO convertToItemVO(SalesOrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}
