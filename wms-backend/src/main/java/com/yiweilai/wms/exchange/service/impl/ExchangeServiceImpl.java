package com.yiweilai.wms.exchange.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.exchange.dto.ExchangeCheckDTO;
import com.yiweilai.wms.exchange.dto.ExchangeCreateDTO;
import com.yiweilai.wms.exchange.dto.ExchangeQueryDTO;
import com.yiweilai.wms.exchange.entity.ExchangeOrder;
import com.yiweilai.wms.exchange.entity.ExchangeOrderItem;
import com.yiweilai.wms.exchange.mapper.ExchangeOrderItemMapper;
import com.yiweilai.wms.exchange.mapper.ExchangeOrderMapper;
import com.yiweilai.wms.exchange.service.ExchangeService;
import com.yiweilai.wms.exchange.vo.ExchangeOrderItemVO;
import com.yiweilai.wms.exchange.vo.ExchangeOrderVO;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.outbound.entity.OutboundOrder;
import com.yiweilai.wms.outbound.entity.OutboundOrderItem;
import com.yiweilai.wms.outbound.mapper.OutboundOrderItemMapper;
import com.yiweilai.wms.outbound.mapper.OutboundOrderMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 换货 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl implements ExchangeService {

    private static final AtomicLong EXCHANGE_NO_SEQUENCE = new AtomicLong();

    private final ExchangeOrderMapper exchangeOrderMapper;
    private final ExchangeOrderItemMapper exchangeOrderItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<ExchangeOrderVO> findByPage(ExchangeQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<ExchangeOrder> orders = exchangeOrderMapper.findByPage(
                query.getExchangeNo(), query.getOrderNo(), query.getPlatformOrderNo(),
                query.getStatus(), query.getWarehouseId());

        PageInfo<ExchangeOrder> pageInfo = new PageInfo<>(orders);

        List<ExchangeOrderVO> voList = orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<ExchangeOrderVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ExchangeOrderVO getById(Long id) {
        ExchangeOrder order = exchangeOrderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        ExchangeOrderVO vo = convertToVO(order);

        // 查询换货明细
        List<ExchangeOrderItemVO> items = exchangeOrderItemMapper.findByExchangeId(id).stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExchangeCreateDTO dto) {
        // 查询原订单
        SalesOrder order = salesOrderMapper.findById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 校验原订单状态必须是已发货
        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "只有已发货的订单才能发起换货");
        }

        // 生成换货单号
        String exchangeNo = "EX"
                + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + String.format("%06d", EXCHANGE_NO_SEQUENCE.incrementAndGet());

        // 创建换货单
        ExchangeOrder exchangeOrder = new ExchangeOrder();
        exchangeOrder.setExchangeNo(exchangeNo);
        exchangeOrder.setOrderId(order.getId());
        exchangeOrder.setOrderNo(order.getOrderNo());
        exchangeOrder.setWarehouseId(dto.getWarehouseId());
        exchangeOrder.setStatus("PENDING_RETURN");
        exchangeOrder.setReason(dto.getReason());
        exchangeOrder.setReturnTrackingNo(dto.getReturnTrackingNo());
        exchangeOrder.setExpressCompanyId(dto.getExpressCompanyId());
        exchangeOrder.setShippingFee(dto.getShippingFee());
        exchangeOrder.setRemark(dto.getRemark());
        exchangeOrderMapper.insert(exchangeOrder);

        // 创建换货明细
        for (ExchangeCreateDTO.ExchangeItemDTO itemDTO : dto.getItems()) {
            ExchangeOrderItem item = new ExchangeOrderItem();
            item.setExchangeId(exchangeOrder.getId());
            item.setSkuId(itemDTO.getSkuId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setSkuName(itemDTO.getSkuName());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setItemType(itemDTO.getItemType());
            exchangeOrderItemMapper.insert(item);
        }

        // 更新原订单状态为换货中
        salesOrderMapper.updateStatus(order.getId(), "EXCHANGING");

        return exchangeOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long exchangeId) {
        ExchangeOrder order = exchangeOrderMapper.findById(exchangeId);
        if (order == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        // 校验状态：必须是待退回状态
        if (!"PENDING_RETURN".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.EXCHANGE_STATUS_ERROR, "只有待退回状态的换货单才能收货");
        }

        // 更新换货单状态为已退回
        exchangeOrderMapper.updateStatus(exchangeId, "RETURNED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void check(ExchangeCheckDTO dto) {
        ExchangeOrder order = exchangeOrderMapper.findById(dto.getExchangeId());
        if (order == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        // 校验状态：待退回或已退回都可以质检
        String status = order.getStatus();
        if (!"PENDING_RETURN".equals(status) && !"RETURNED".equals(status)) {
            throw new BusinessException(ErrorCode.EXCHANGE_STATUS_ERROR, "只有待退回或已退回状态的换货单才能质检");
        }

        // 查询次品仓和报废仓
        Warehouse defectiveWarehouse = warehouseMapper.findByType("DEFECTIVE");
        Warehouse scrapWarehouse = warehouseMapper.findByType("SCRAP");

        // 更新每个退回商品的质量状态并入库
        for (ExchangeCheckDTO.ExchangeCheckItemDTO checkItem : dto.getItems()) {
            exchangeOrderItemMapper.updateQualityStatus(checkItem.getItemId(), checkItem.getQualityStatus());

            // 查询退回商品明细
            ExchangeOrderItem item = exchangeOrderItemMapper.findById(checkItem.getItemId());
            if (item == null || !"RETURN_ITEM".equals(item.getItemType())) {
                continue;
            }

            // 根据质检结果入库到对应仓库
            String qualityStatus = checkItem.getQualityStatus();
            Long targetWarehouseId;

            if ("DEFECTIVE".equals(qualityStatus)) {
                if (defectiveWarehouse == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置次品仓，请先在仓库管理中创建类型为「次品仓」的仓库");
                }
                targetWarehouseId = defectiveWarehouse.getId();
            } else if ("SCRAPPED".equals(qualityStatus)) {
                if (scrapWarehouse == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置报废仓，请先在仓库管理中创建类型为「报废仓」的仓库");
                }
                targetWarehouseId = scrapWarehouse.getId();
            } else {
                // 可售商品退回原发货仓
                targetWarehouseId = order.getWarehouseId();
            }

            // 增加库存
            addStock(item.getSkuId(), item.getQuantity(),
                    order.getExchangeNo(), targetWarehouseId, qualityStatus);
        }

        // 更新换货单状态为已质检
        exchangeOrderMapper.updateStatus(dto.getExchangeId(), "CHECKED");
    }

    /**
     * 增加库存
     */
    private void addStock(Long skuId, int quantity,
                          String exchangeNo, Long warehouseId, String qualityStatus) {
        // 查询或创建库存记录
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, warehouseId);

        if (stock == null) {
            // 新增库存记录
            stock = new Stock();
            stock.setSkuId(skuId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stock.setLockedQty(0);
            stockMapper.insert(stock);
        } else {
            // 增加库存
            stockMapper.addQuantity(stock.getId(), quantity);
        }

        // 写库存流水
        StockLog log = new StockLog();
        log.setBizType("EXCHANGE");
        log.setBizNo(exchangeNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(stock.getQuantity());
        log.setQuantityChange(quantity);
        log.setQuantityAfter(stock.getQuantity() + quantity);
        log.setRemark("换货退回入库 - " + qualityStatus);
        stockLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long exchangeId, Long expressCompanyId, String trackingNo, BigDecimal shippingFee) {
        ExchangeOrder order = exchangeOrderMapper.findById(exchangeId);
        if (order == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        // 校验状态：必须是已质检状态
        if (!"CHECKED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.EXCHANGE_STATUS_ERROR, "只有已质检状态的换货单才能发货");
        }

        // 更新换货单的快递信息
        if (expressCompanyId != null || trackingNo != null || shippingFee != null) {
            exchangeOrderMapper.updateExpressInfo(exchangeId, expressCompanyId, trackingNo, shippingFee);
        }

        // 查询换货明细
        List<ExchangeOrderItem> allItems = exchangeOrderItemMapper.findByExchangeId(exchangeId);

        // 筛选出换出商品（EXCHANGE_ITEM）且质量为可售的
        List<ExchangeOrderItem> exchangeItems = allItems.stream()
                .filter(item -> "EXCHANGE_ITEM".equals(item.getItemType())
                        && (item.getQualityStatus() == null || "SELLABLE".equals(item.getQualityStatus())))
                .collect(Collectors.toList());

        if (exchangeItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可发货的换出商品");
        }

        // 生成出库单号
        String outboundNo = "OB"
                + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + String.format("%06d", EXCHANGE_NO_SEQUENCE.incrementAndGet());

        // 创建出库单
        OutboundOrder outboundOrder = new OutboundOrder();
        outboundOrder.setOutboundNo(outboundNo);
        outboundOrder.setOrderId(order.getOrderId());
        outboundOrder.setOrderNo(order.getOrderNo());
        outboundOrder.setWarehouseId(order.getWarehouseId());
        outboundOrder.setExpressCompanyId(expressCompanyId);
        outboundOrder.setTrackingNo(trackingNo);
        outboundOrder.setShippingFee(shippingFee);
        outboundOrder.setStatus("WAIT_PICKING");
        outboundOrder.setRemark("换货单[" + order.getExchangeNo() + "]自动创建 - 换货快递费");
        outboundOrderMapper.insert(outboundOrder);

        // 创建出库明细并扣减库存
        for (ExchangeOrderItem item : exchangeItems) {
            OutboundOrderItem outboundItem = new OutboundOrderItem();
            outboundItem.setOutboundId(outboundOrder.getId());
            outboundItem.setSkuId(item.getSkuId());
            outboundItem.setSkuCode(item.getSkuCode());
            outboundItem.setSkuName(item.getSkuName());
            outboundItem.setQuantity(item.getQuantity());
            outboundOrderItemMapper.insert(outboundItem);

            // 扣减库存
            deductStock(item.getSkuId(), item.getQuantity(),
                    outboundNo, order.getWarehouseId());
        }

        // 更新换货单状态为已换货
        exchangeOrderMapper.updateStatus(exchangeId, "EXCHANGED");

        // 把原出库单标记为已换货（不参与销量统计）
        OutboundOrder originalOutbound = outboundOrderMapper.findByOrderIdAndStatus(order.getOrderId(), "SHIPPED");
        if (originalOutbound != null) {
            outboundOrderMapper.updateStatus(originalOutbound.getId(), "EXCHANGED");
        }

        // 更新原订单明细
        updateOrderItems(order.getOrderId(), allItems);

        // 更新原订单状态为已换货
        salesOrderMapper.updateStatus(order.getOrderId(), "EXCHANGED");
    }

    /**
     * 换货发货后更新原订单明细
     */
    private void updateOrderItems(Long orderId, List<ExchangeOrderItem> exchangeItems) {
        // 获取退回商品（要从原订单中移除的）
        List<ExchangeOrderItem> returnItems = exchangeItems.stream()
                .filter(item -> "RETURN_ITEM".equals(item.getItemType()))
                .collect(Collectors.toList());

        // 获取换出商品（要添加到原订单的）
        List<ExchangeOrderItem> newItems = exchangeItems.stream()
                .filter(item -> "EXCHANGE_ITEM".equals(item.getItemType()))
                .collect(Collectors.toList());

        // 查询原订单明细
        List<SalesOrderItem> orderItems = salesOrderItemMapper.findByOrderId(orderId);

        // 删除退回的商品（按SKU匹配）
        for (ExchangeOrderItem returnItem : returnItems) {
            SalesOrderItem matchedItem = orderItems.stream()
                    .filter(oi -> oi.getSkuId().equals(returnItem.getSkuId()))
                    .findFirst()
                    .orElse(null);
            if (matchedItem != null) {
                salesOrderItemMapper.deleteById(matchedItem.getId());
            }
        }

        // 添加换出的商品
        for (ExchangeOrderItem newItem : newItems) {
            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setSkuId(newItem.getSkuId());
            orderItem.setSkuCode(newItem.getSkuCode());
            orderItem.setSkuName(newItem.getSkuName());
            orderItem.setSizeValue(newItem.getSizeValue());
            orderItem.setQuantity(newItem.getQuantity());
            BigDecimal unitPrice = newItem.getUnitPrice() != null ? newItem.getUnitPrice() : BigDecimal.ZERO;
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(newItem.getQuantity())));
            salesOrderItemMapper.insert(orderItem);
        }
    }

    /**
     * 扣减库存
     */
    private void deductStock(Long skuId, int quantity,
                             String outboundNo, Long warehouseId) {
        // 查询库存记录
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, warehouseId);

        if (stock == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不足，SKU: " + skuId);
        }

        // 检查库存是否充足
        if (stock.getQuantity() < quantity) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "库存不足，SKU: " + skuId + "，可用: " + stock.getQuantity() + "，需要: " + quantity);
        }

        // 扣减库存
        stockMapper.deductQuantity(stock.getId(), quantity);

        // 写库存流水
        StockLog log = new StockLog();
        log.setBizType("EXCHANGE");
        log.setBizNo(outboundNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(stock.getQuantity());
        log.setQuantityChange(-quantity);
        log.setQuantityAfter(stock.getQuantity() - quantity);
        log.setRemark("换货发出出库");
        stockLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long exchangeId) {
        ExchangeOrder order = exchangeOrderMapper.findById(exchangeId);
        if (order == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_FOUND);
        }

        // 校验状态：只有 PENDING_RETURN/RETURNED/CHECKED 可以取消
        String status = order.getStatus();
        if (!"PENDING_RETURN".equals(status)
                && !"RETURNED".equals(status)
                && !"CHECKED".equals(status)) {
            throw new BusinessException(ErrorCode.EXCHANGE_STATUS_ERROR, "当前状态不允许取消");
        }

        // 更新换货单状态为已取消
        exchangeOrderMapper.updateStatus(exchangeId, "CANCELLED");

        // 恢复原订单状态为已发货
        salesOrderMapper.updateStatus(order.getOrderId(), "SHIPPED");
    }

    private ExchangeOrderVO convertToVO(ExchangeOrder order) {
        ExchangeOrderVO vo = new ExchangeOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private ExchangeOrderItemVO convertToItemVO(ExchangeOrderItem item) {
        ExchangeOrderItemVO vo = new ExchangeOrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}