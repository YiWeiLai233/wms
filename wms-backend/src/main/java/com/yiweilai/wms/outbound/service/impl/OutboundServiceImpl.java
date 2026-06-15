package com.yiweilai.wms.outbound.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.outbound.dto.OutboundBatchCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
import com.yiweilai.wms.outbound.dto.OutboundUpdateDTO;
import com.yiweilai.wms.outbound.entity.OutboundOrder;
import com.yiweilai.wms.outbound.entity.OutboundOrderItem;
import com.yiweilai.wms.outbound.mapper.OutboundOrderItemMapper;
import com.yiweilai.wms.outbound.mapper.OutboundOrderMapper;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.outbound.vo.OutboundOrderItemVO;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductBarcode;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductBarcodeMapper;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.express.entity.ExpressFeeStep;
import com.yiweilai.wms.express.entity.ExpressFeeTemplate;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 出库 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundServiceImpl implements OutboundService {

    private static final AtomicLong OUTBOUND_NO_SEQUENCE = new AtomicLong();

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final ProductBarcodeMapper productBarcodeMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseShelfMapper shelfMapper;
    private final ExpressFeeStepMapper feeStepMapper;
    private final ExpressFeeTemplateMapper feeTemplateMapper;

    @Override
    public PageResult<OutboundOrderVO> findByPage(OutboundQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<OutboundOrder> orders = outboundOrderMapper.findByPage(
                query.getOutboundNo(), query.getOrderNo(), query.getPlatformOrderNo(), query.getTrackingNo(),
                query.getStatus(), query.getWarehouseId());

        PageInfo<OutboundOrder> pageInfo = new PageInfo<>(orders);

        List<OutboundOrderVO> voList = orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<OutboundOrderVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public OutboundOrderVO getById(Long id) {
        OutboundOrder order = outboundOrderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND);
        }

        OutboundOrderVO vo = convertToVO(order);

        // 查询仓库信息
        Long warehouseId = order.getWarehouseId();
        String warehouseName = null;
        if (warehouseId != null) {
            Warehouse warehouse = warehouseMapper.findById(warehouseId);
            if (warehouse != null) {
                warehouseName = warehouse.getName();
            }
        }

        // 查询出库明细
        final String finalWarehouseName = warehouseName;
        List<OutboundOrderItemVO> items = outboundOrderItemMapper.findByOutboundId(id).stream()
                .map(item -> convertToItemVO(item, warehouseId, finalWarehouseName))
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OutboundCreateDTO dto) {
        return createForOrder(dto.getOrderId(), dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createBatch(OutboundBatchCreateDTO dto) {
        if (dto.getOrderIds() == null || dto.getOrderIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单ID列表不能为空");
        }

        List<Long> outboundIds = new ArrayList<>();
        for (Long orderId : new LinkedHashSet<>(dto.getOrderIds())) {
            if (orderId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "订单ID不能为空");
            }
            outboundIds.add(createForOrder(orderId, dto.getRemark()));
        }
        return outboundIds;
    }

    private Long createForOrder(Long orderId, String remark) {
        // 查询订单
        SalesOrder order = salesOrderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 检查订单状态
        if (!"WAIT_OUTBOUND".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待出库");
        }

        // 检查是否已有出库单
        OutboundOrder existing = outboundOrderMapper.findByOrderId(orderId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该订单已有出库单");
        }

        // 生成出库单号
        String outboundNo = generateOutboundNo();

        // 创建出库单
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOutboundNo(outboundNo);
        outboundOrder.setOrderId(order.getId());
        outboundOrder.setOrderNo(order.getOrderNo());
        outboundOrder.setWarehouseId(order.getWarehouseId());
        outboundOrder.setStatus("WAIT_PICKING");
        outboundOrder.setRemark(remark);
        outboundOrderMapper.insert(outboundOrder);

        // 创建出库明细（从订单明细复制）
        List<SalesOrderItem> orderItems = salesOrderItemMapper.findByOrderId(order.getId());
        for (SalesOrderItem orderItem : orderItems) {
            OutboundOrderItem outboundItem = new OutboundOrderItem();
            outboundItem.setOutboundId(outboundOrder.getId());
            outboundItem.setSkuId(orderItem.getSkuId());
            outboundItem.setSkuCode(orderItem.getSkuCode());
            outboundItem.setSkuName(orderItem.getSkuName());
            outboundItem.setQuantity(orderItem.getQuantity());
            outboundOrderItemMapper.insert(outboundItem);
        }

        // 库存在订单导入时已扣减，此处不再扣减

        // 更新订单状态为出库中
        salesOrderMapper.updateStatus(order.getId(), "OUTBOUNDING");

        return outboundOrder.getId();
    }

    private String generateOutboundNo() {
        return "OB"
                + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + String.format("%06d", OUTBOUND_NO_SEQUENCE.incrementAndGet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scan(OutboundScanDTO dto) {
        OutboundOrder order = outboundOrderMapper.findById(dto.getOutboundId());
        if (order == null) {
            throw new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND);
        }

        if (!"WAIT_PICKING".equals(order.getStatus()) && !"PICKING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OUTBOUND_STATUS_ERROR, "出库单状态不允许扫码");
        }

        // 通过SKU编码或条码查找商品
        String skuCode = dto.getScanCode();
        ProductSku sku = productSkuMapper.findBySkuCode(skuCode);

        // 如果SKU编码找不到，尝试通过条码查找
        if (sku == null) {
            ProductBarcode barcode = productBarcodeMapper.findByBarcode(skuCode);
            if (barcode != null) {
                sku = productSkuMapper.findById(barcode.getSkuId());
            }
        }

        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND, "未找到对应的SKU");
        }

        // 查找出库明细
        OutboundOrderItem item = outboundOrderItemMapper.findByOutboundIdAndSkuCode(
                dto.getOutboundId(), sku.getSkuCode());
        if (item == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该SKU不在出库单中");
        }

        // 更新已拣数量和扫码状态
        int currentPickedQty = item.getPickedQty() == null ? 0 : item.getPickedQty();
        if (currentPickedQty >= item.getQuantity()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该SKU已完成扫码: " + item.getSkuCode());
        }

        int newPickedQty = currentPickedQty + 1;
        outboundOrderItemMapper.updatePickedQty(item.getId(), newPickedQty);
        outboundOrderItemMapper.updateScanned(item.getId(), newPickedQty >= item.getQuantity() ? 1 : 0);
        outboundOrderItemMapper.updateShelfId(item.getId(), dto.getShelfId());

        // 更新出库单状态为拣货中
        if ("WAIT_PICKING".equals(order.getStatus())) {
            outboundOrderMapper.updateStatus(dto.getOutboundId(), "PICKING");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(OutboundConfirmDTO dto) {
        OutboundOrder order = outboundOrderMapper.findById(dto.getOutboundId());
        if (order == null) {
            throw new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND);
        }

        // 只有 WAIT_PICKING / PICKING 状态才能确认出库
        if (!"WAIT_PICKING".equals(order.getStatus()) && !"PICKING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OUTBOUND_STATUS_ERROR, "出库单状态不允许确认出库");
        }

        List<OutboundOrderItem> items = outboundOrderItemMapper.findByOutboundId(dto.getOutboundId());

        // 库存在创建出库单时已扣减，此处不再重复扣减

        // 计算快递费用
        BigDecimal shippingFee = dto.getShippingFee();
        BigDecimal weight = dto.getEstimatedWeight();

        // 如果提供了重量但没有提供费用，根据模板计算
        if (shippingFee == null && weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            shippingFee = calculateFeeByTemplate(dto.getFeeTemplateId(), weight);
        }

        // 如果都没有提供，使用默认计算
        if (shippingFee == null) {
            shippingFee = calculateShippingFee(items);
        }

        // 更新快递信息
        outboundOrderMapper.updateExpressInfo(dto.getOutboundId(), dto.getExpressCompanyId(), dto.getTrackingNo(), shippingFee);

        // 更新出库单状态为已发货
        outboundOrderMapper.updateStatus(dto.getOutboundId(), "SHIPPED");
        outboundOrderMapper.updateShippedAt(dto.getOutboundId());

        // 更新订单状态为已发货
        salesOrderMapper.updateStatus(order.getOrderId(), "SHIPPED");
        salesOrderMapper.updateShippedAt(order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OutboundUpdateDTO dto) {
        OutboundOrder order = outboundOrderMapper.findById(dto.getId());
        if (order == null) {
            throw new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND);
        }

        // 只有已发货状态可以编辑
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OUTBOUND_STATUS_ERROR, "只有已发货状态的发货单可以编辑");
        }

        // 更新快递信息
        if (dto.getExpressCompanyId() != null || dto.getTrackingNo() != null || dto.getShippingFee() != null) {
            outboundOrderMapper.updateExpressInfo(dto.getId(), dto.getExpressCompanyId(), dto.getTrackingNo(), dto.getShippingFee());
        }

        // 更新备注
        if (dto.getRemark() != null) {
            outboundOrderMapper.updateRemark(dto.getId(), dto.getRemark());
        }
    }

    /**
     * 根据模板计算快递费用
     */
    private BigDecimal calculateFeeByTemplate(Long templateId, BigDecimal weight) {
        if (templateId == null) {
            // 使用默认模板
            var defaultTemplate = feeTemplateMapper.findDefault();
            if (defaultTemplate != null) {
                templateId = defaultTemplate.getId();
            } else {
                return null;
            }
        }

        // 查询模板信息
        var template = feeTemplateMapper.findById(templateId);
        if (template == null) {
            return null;
        }

        // 首重续重类型
        if ("FIRST_CONTINUE".equals(template.getTemplateType())) {
            return calculateFirstContinueFee(template, weight);
        }

        // 阶梯计费类型（默认）
        ExpressFeeStep step = feeStepMapper.findByTemplateIdAndWeight(templateId, weight);
        if (step != null) {
            return step.getFee();
        }

        return null;
    }

    /**
     * 首重续重计费
     */
    private BigDecimal calculateFirstContinueFee(ExpressFeeTemplate template, BigDecimal weight) {
        if (template.getFirstWeight() == null || template.getFirstFee() == null) {
            return null;
        }

        BigDecimal firstWeight = template.getFirstWeight();
        BigDecimal firstFee = template.getFirstFee();

        // 重量不超过首重
        if (weight.compareTo(firstWeight) <= 0) {
            return firstFee;
        }

        // 超过首重，计算续重费用
        if (template.getAdditionalWeight() == null || template.getAdditionalFee() == null) {
            return firstFee;
        }

        BigDecimal additionalWeight = weight.subtract(firstWeight);
        // 向上取整续重重量
        BigDecimal additionalUnits = additionalWeight.divide(template.getAdditionalWeight(), 0, java.math.RoundingMode.UP);
        BigDecimal additionalFee = additionalUnits.multiply(template.getAdditionalFee());

        return firstFee.add(additionalFee);
    }

    /**
     * 计算快递费用
     * 首重1kg：12元，续重：5元/kg
     */
    private BigDecimal calculateShippingFee(List<OutboundOrderItem> items) {
        // 计算总重量
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (OutboundOrderItem item : items) {
            ProductSku sku = productSkuMapper.findById(item.getSkuId());
            if (sku != null && sku.getWeight() != null) {
                totalWeight = totalWeight.add(sku.getWeight().multiply(new BigDecimal(item.getQuantity())));
            }
        }

        // 如果没有重量信息，默认1kg
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalWeight = new BigDecimal("1");
        }

        // 计算费用：首重12元，续重5元/kg
        BigDecimal firstWeightFee = new BigDecimal("12");
        BigDecimal additionalWeightFee = new BigDecimal("5");
        BigDecimal firstWeight = new BigDecimal("1");

        BigDecimal fee = firstWeightFee;
        if (totalWeight.compareTo(firstWeight) > 0) {
            BigDecimal additionalWeight = totalWeight.subtract(firstWeight);
            // 向上取整
            int additionalKg = additionalWeight.intValue();
            if (additionalWeight.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
                additionalKg++;
            }
            fee = fee.add(additionalWeightFee.multiply(new BigDecimal(additionalKg)));
        }

        return fee;
    }

    /**
     * 扣减库存（核心方法，每次扣减前重新查询避免数据过期）
     */
    private void deductStock(Long skuId, int quantity, String outboundNo, Long warehouseId) {
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

                writeOutboundLog(skuId, warehouseId, beforeQty, deductQty, outboundNo);
                remaining -= deductQty;
                deducted = true;
                break;
            }

            if (!deducted) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "SKU[" + skuId + "]库存不足，剩余需扣: " + remaining);
            }
        }
    }

    private void writeOutboundLog(Long skuId, Long warehouseId, int beforeQty,
                                  int deductQty, String outboundNo) {
        StockLog log = new StockLog();
        log.setBizType("OUTBOUND");
        log.setBizNo(outboundNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(beforeQty);
        log.setQuantityChange(-deductQty);
        log.setQuantityAfter(beforeQty - deductQty);
        log.setRemark("出库扣减");
        stockLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        OutboundOrder order = outboundOrderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND);
        }
        if ("SHIPPED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已发货的出库单不能取消");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "出库单已取消");
        }

        // 恢复库存
        List<OutboundOrderItem> items = outboundOrderItemMapper.findByOutboundId(id);
        for (OutboundOrderItem item : items) {
            restoreStock(item.getSkuId(), item.getQuantity(), order.getOutboundNo(), order.getWarehouseId());
        }

        // 更新出库单状态为已取消
        outboundOrderMapper.updateStatus(id, "CANCELLED");

        // 更新关联订单状态为出库失败
        salesOrderMapper.updateStatus(order.getOrderId(), "OUTBOUND_FAILED");
    }

    /**
     * 恢复库存（取消出库时调用）
     */
    private void restoreStock(Long skuId, int quantity, String outboundNo, Long warehouseId) {
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, warehouseId);
        int beforeQty;
        if (stock != null) {
            beforeQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            stockMapper.addQuantity(stock.getId(), quantity);
        } else {
            beforeQty = 0;
            stock = new Stock();
            stock.setSkuId(skuId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stock.setLockedQty(0);
//            stock.setDefectiveQty(0);
            stockMapper.insert(stock);
        }

        // 写库存流水
        StockLog log = new StockLog();
        log.setBizType("RETURN");
        log.setBizNo(outboundNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(beforeQty);
        log.setQuantityChange(quantity);
        log.setQuantityAfter(beforeQty + quantity);
        log.setRemark("取消出库恢复库存");
        stockLogMapper.insert(log);
    }

    private OutboundOrderVO convertToVO(OutboundOrder order) {
        OutboundOrderVO vo = new OutboundOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private OutboundOrderItemVO convertToItemVO(OutboundOrderItem item, Long warehouseId, String warehouseName) {
        OutboundOrderItemVO vo = new OutboundOrderItemVO();
        BeanUtils.copyProperties(item, vo);

        // 设置仓库信息
        vo.setWarehouseId(warehouseId);
        vo.setWarehouseName(warehouseName);

        // 查询SKU图片
        if (item.getSkuId() != null) {
            ProductSku sku = productSkuMapper.findById(item.getSkuId());
            if (sku != null) {
                String image = sku.getImage();
                // 如果SKU没有图片，查询SPU主图
                if (image == null || image.isEmpty()) {
                    Product product = productMapper.findById(sku.getProductId());
                    if (product != null) {
                        image = product.getMainImage();
                    }
                }
                vo.setSkuImage(image);
            }
        }

        // 查询货架编码
        if (item.getShelfId() != null) {
            WarehouseShelf shelf = shelfMapper.findById(item.getShelfId());
            if (shelf != null) {
                vo.setShelfCode(shelf.getCode());
            }
        }
        return vo;
    }
}
