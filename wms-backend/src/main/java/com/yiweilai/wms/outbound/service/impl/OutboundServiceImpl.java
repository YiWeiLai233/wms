package com.yiweilai.wms.outbound.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
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
import com.yiweilai.wms.product.entity.ProductBarcode;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductBarcodeMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.WarehouseLocation;
import com.yiweilai.wms.warehouse.mapper.WarehouseLocationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 出库 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundServiceImpl implements OutboundService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductBarcodeMapper productBarcodeMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final WarehouseLocationMapper locationMapper;

    @Override
    public PageResult<OutboundOrderVO> findByPage(OutboundQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<OutboundOrder> orders = outboundOrderMapper.findByPage(
                query.getOutboundNo(), query.getOrderNo(),
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

        // 查询出库明细
        List<OutboundOrderItemVO> items = outboundOrderItemMapper.findByOutboundId(id).stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OutboundCreateDTO dto) {
        // 查询订单
        SalesOrder order = salesOrderMapper.findById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 检查订单状态
        if (!"WAIT_OUTBOUND".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不是待出库");
        }

        // 检查是否已有出库单
        OutboundOrder existing = outboundOrderMapper.findByOrderId(dto.getOrderId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该订单已有出库单");
        }

        // 生成出库单号
        String outboundNo = "OB" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // 创建出库单
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOutboundNo(outboundNo);
        outboundOrder.setOrderId(order.getId());
        outboundOrder.setOrderNo(order.getOrderNo());
        outboundOrder.setWarehouseId(order.getWarehouseId());
        outboundOrder.setStatus("WAIT_PICKING");
        outboundOrder.setRemark(dto.getRemark());
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

        // 更新订单状态为出库中
        salesOrderMapper.updateStatus(order.getId(), "OUTBOUNDING");

        return outboundOrder.getId();
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
        outboundOrderItemMapper.updateLocationId(item.getId(), dto.getLocationId());

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

        // 检查所有明细是否都已扫码
        List<OutboundOrderItem> items = outboundOrderItemMapper.findByOutboundId(dto.getOutboundId());
        for (OutboundOrderItem item : items) {
            int pickedQty = item.getPickedQty() == null ? 0 : item.getPickedQty();
            if (pickedQty < item.getQuantity()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "存在未扫码确认的商品: " + item.getSkuCode());
            }
            if (item.getLocationId() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "商品未选择出库库位: " + item.getSkuCode());
            }
        }

        // 扣减库存
        for (OutboundOrderItem item : items) {
            deductStock(item.getSkuId(), item.getLocationId(), item.getQuantity(),
                    order.getOutboundNo(), order.getWarehouseId());
        }

        // 更新出库单状态为已发货
        outboundOrderMapper.updateStatus(dto.getOutboundId(), "SHIPPED");
        outboundOrderMapper.updateShippedAt(dto.getOutboundId());

        // 更新订单状态为已发货
        salesOrderMapper.updateStatus(order.getOrderId(), "SHIPPED");
        salesOrderMapper.updateShippedAt(order.getOrderId());
    }

    /**
     * 扣减库存（核心方法）
     */
    private void deductStock(Long skuId, Long locationId, int quantity,
                             String outboundNo, Long warehouseId) {
        // 查询库存
        Stock stock = stockMapper.findBySkuAndLocation(skuId, locationId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "库存记录不存在");
        }

        // 扣减库存（防负数）
        int affected = stockMapper.deductQuantity(stock.getId(), quantity);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足");
        }

        // 写库存流水
        StockLog log = new StockLog();
        log.setBizType("OUTBOUND");
        log.setBizNo(outboundNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setLocationId(locationId);
        log.setQuantityBefore(stock.getQuantity());
        log.setQuantityChange(-quantity);
        log.setQuantityAfter(stock.getQuantity() - quantity);
        log.setRemark("出库扣减");
        stockLogMapper.insert(log);
    }

    private OutboundOrderVO convertToVO(OutboundOrder order) {
        OutboundOrderVO vo = new OutboundOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private OutboundOrderItemVO convertToItemVO(OutboundOrderItem item) {
        OutboundOrderItemVO vo = new OutboundOrderItemVO();
        BeanUtils.copyProperties(item, vo);

        // 查询库位编码
        if (item.getLocationId() != null) {
            WarehouseLocation location = locationMapper.findById(item.getLocationId());
            if (location != null) {
                vo.setLocationCode(location.getCode());
            }
        }
        return vo;
    }
}
