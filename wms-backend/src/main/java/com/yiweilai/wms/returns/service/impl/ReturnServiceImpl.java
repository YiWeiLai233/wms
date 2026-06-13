package com.yiweilai.wms.returns.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.entity.ReturnOrder;
import com.yiweilai.wms.returns.entity.ReturnOrderItem;
import com.yiweilai.wms.returns.mapper.ReturnOrderItemMapper;
import com.yiweilai.wms.returns.mapper.ReturnOrderMapper;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderItemVO;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退货 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnOrderItemMapper returnOrderItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final ProductSkuMapper productSkuMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<ReturnOrderVO> findByPage(ReturnQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<ReturnOrder> orders = returnOrderMapper.findByPage(
                query.getReturnNo(), query.getOrderNo(), query.getPlatformOrderNo(),
                query.getStatus(), query.getWarehouseId());

        PageInfo<ReturnOrder> pageInfo = new PageInfo<>(orders);

        List<ReturnOrderVO> voList = orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<ReturnOrderVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    @Override
    public ReturnOrderVO getById(Long id) {
        ReturnOrder order = returnOrderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_FOUND);
        }

        ReturnOrderVO vo = convertToVO(order);

        // 查询退货明细
        List<ReturnOrderItemVO> items = returnOrderItemMapper.findByReturnId(id).stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());
        vo.setItems(items);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReturnCreateDTO dto) {
        // 查询订单
        SalesOrder order = salesOrderMapper.findById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 检查订单状态（已发货才能退货）
        if (!"SHIPPED".equals(order.getOrderStatus()) && !"RETURNING".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不允许退货");
        }

        // 生成退货单号
        String returnNo = "RT" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // 创建退货单
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setReturnNo(returnNo);
        returnOrder.setOrderId(order.getId());
        returnOrder.setOrderNo(order.getOrderNo());
        returnOrder.setWarehouseId(order.getWarehouseId());
        returnOrder.setStatus("PENDING_CHECK");
        returnOrder.setReason(dto.getReason());
        returnOrder.setRemark(dto.getRemark());
        returnOrderMapper.insert(returnOrder);

        // 创建退货明细
        if (dto.getItems() != null) {
            for (ReturnCreateDTO.ReturnItemDTO itemDTO : dto.getItems()) {
                ProductSku sku = productSkuMapper.findById(itemDTO.getSkuId());
                if (sku == null) {
                    throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
                }

                ReturnOrderItem item = new ReturnOrderItem();
                item.setReturnId(returnOrder.getId());
                item.setSkuId(sku.getId());
                item.setSkuCode(sku.getSkuCode());
                item.setSkuName(sku.getName());
                item.setQuantity(itemDTO.getQuantity());
                returnOrderItemMapper.insert(item);
            }
        }

        // 更新订单状态为退货中
        salesOrderMapper.updateStatus(order.getId(), "RETURNING");

        return returnOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void check(ReturnCheckDTO dto) {
        ReturnOrder order = returnOrderMapper.findById(dto.getReturnId());
        if (order == null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_FOUND);
        }

        if (!"PENDING_CHECK".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.RETURN_STATUS_ERROR, "退货单状态不是待质检");
        }

        // 更新每个明细的质检状态
        for (ReturnCheckDTO.ReturnCheckItemDTO itemDTO : dto.getItems()) {
            ReturnOrderItem item = returnOrderItemMapper.findById(itemDTO.getItemId());
            if (item == null) {
                continue;
            }
            returnOrderItemMapper.updateQualityStatus(
                    item.getId(), itemDTO.getQualityStatus());
        }

        // 更新退货单状态为可售（如果有可售的）
        boolean hasSellable = dto.getItems().stream()
                .anyMatch(item -> "SELLABLE".equals(item.getQualityStatus()));
        if (hasSellable) {
            returnOrderMapper.updateStatus(dto.getReturnId(), "SELLABLE");
        } else {
            returnOrderMapper.updateStatus(dto.getReturnId(), "DEFECTIVE");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long returnId) {
        ReturnOrder order = returnOrderMapper.findById(returnId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_FOUND);
        }

        if (!"SELLABLE".equals(order.getStatus()) && !"DEFECTIVE".equals(order.getStatus())
                && !"SCRAPPED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.RETURN_STATUS_ERROR, "退货单状态不允许确认入库");
        }

        // 查询次品仓和报废仓
        Warehouse defectiveWarehouse = warehouseMapper.findByType("DEFECTIVE");
        Warehouse scrapWarehouse = warehouseMapper.findByType("SCRAP");

        // 查询退货明细
        List<ReturnOrderItem> items = returnOrderItemMapper.findByReturnId(returnId);

        // 按质检结果路由到对应仓库入库
        for (ReturnOrderItem item : items) {
            String qualityStatus = item.getQualityStatus();
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

            addStock(item.getSkuId(), item.getQuantity(),
                    order.getReturnNo(), targetWarehouseId, qualityStatus);
        }

        // 更新退货单状态
        returnOrderMapper.updateStatus(returnId, "COMPLETED");

        // 更新订单状态
        salesOrderMapper.updateStatus(order.getOrderId(), "RETURNED");
    }

    /**
     * 增加库存
     */
    private void addStock(Long skuId, int quantity,
                          String returnNo, Long warehouseId, String qualityStatus) {
        // 查询或创建库存记录
        Stock stock = stockMapper.findBySkuAndWarehouse(skuId, warehouseId);

        if (stock == null) {
            // 新增库存记录
            stock = new Stock();
            stock.setSkuId(skuId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(0);
            stock.setLockedQty(0);
            stock.setDefectiveQty(0);

            if ("DEFECTIVE".equals(qualityStatus) || "SCRAPPED".equals(qualityStatus)) {
                stock.setDefectiveQty(quantity);
            } else {
                stock.setQuantity(quantity);
            }

            stockMapper.insert(stock);
        } else {
            // 增加库存
            if ("DEFECTIVE".equals(qualityStatus) || "SCRAPPED".equals(qualityStatus)) {
                // 次品库存
                stockMapper.addDefectiveQuantity(stock.getId(), quantity);
            } else {
                // 正常库存
                stockMapper.addQuantity(stock.getId(), quantity);
            }
        }

        // 写库存流水
        StockLog log = new StockLog();
        log.setBizType("RETURN");
        log.setBizNo(returnNo);
        log.setSkuId(skuId);
        log.setWarehouseId(warehouseId);
        log.setQuantityBefore(stock.getQuantity());
        log.setQuantityChange(quantity);
        log.setQuantityAfter(stock.getQuantity() + quantity);
        log.setRemark("退货入库");
        stockLogMapper.insert(log);
    }

    private ReturnOrderVO convertToVO(ReturnOrder order) {
        ReturnOrderVO vo = new ReturnOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private ReturnOrderItemVO convertToItemVO(ReturnOrderItem item) {
        ReturnOrderItemVO vo = new ReturnOrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}
