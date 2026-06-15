package com.yiweilai.wms.search.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.outbound.entity.OutboundOrder;
import com.yiweilai.wms.outbound.mapper.OutboundOrderMapper;
import com.yiweilai.wms.privacy.crypto.PrivacyCryptoService;
import com.yiweilai.wms.privacy.crypto.PrivacyHashService;
import com.yiweilai.wms.returns.entity.ReturnOrder;
import com.yiweilai.wms.returns.mapper.ReturnOrderMapper;
import com.yiweilai.wms.search.dto.OrderSearchDTO;
import com.yiweilai.wms.search.service.OrderSearchService;
import com.yiweilai.wms.search.vo.OrderSearchVO;
import com.yiweilai.wms.warehouse.entity.Warehouse;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单搜索 Service 实现
 * 当前基于 MySQL 实现，后期可替换为 Elasticsearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchServiceImpl implements OrderSearchService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final WarehouseMapper warehouseMapper;
    private final PrivacyCryptoService privacyCryptoService;
    private final PrivacyHashService privacyHashService;

    @Override
    public PageResult<OrderSearchVO> search(OrderSearchDTO query) {
        // 当前使用 MySQL 模拟搜索，后期替换为 ES
        PageHelper.startPage(query.getPage(), query.getSize());

        String keyword = query.getKeyword();
        String receiverNameHash = privacyHashService.hmacSha256(privacyHashService.normalizeName(keyword));
        String receiverPhoneHash = privacyHashService.hmacSha256(privacyHashService.normalizePhone(keyword));
        List<SalesOrder> orders = salesOrderMapper.searchByKeyword(
                keyword, receiverNameHash, receiverPhoneHash,
                query.getOrderStatus(), query.getWarehouseId());

        PageInfo<SalesOrder> pageInfo = new PageInfo<>(orders);

        List<OrderSearchVO> voList = orders.stream()
                .map(this::convertToSearchVO)
                .collect(Collectors.toList());

        PageResult<OrderSearchVO> result = new PageResult<>();
        result.setTotal(pageInfo.getTotal());
        result.setList(voList);
        return result;
    }

    private OrderSearchVO convertToSearchVO(SalesOrder order) {
        OrderSearchVO vo = new OrderSearchVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlatformOrderNo(order.getPlatformOrderNo());
        vo.setReceiverName(privacyCryptoService.decrypt(order.getReceiverName()));
        vo.setReceiverPhone(privacyCryptoService.decrypt(order.getReceiverPhone()));
        vo.setReceiverAddress(privacyCryptoService.decrypt(order.getReceiverAddress()));
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setCreatedAt(order.getCreatedAt());

        // 查询仓库名称
        if (order.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.findById(order.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseId(warehouse.getId());
                vo.setWarehouseName(warehouse.getName());
            }
        }

        // 查询订单明细
        List<SalesOrderItem> items = salesOrderItemMapper.findByOrderId(order.getId());
        vo.setSkuCodes(items.stream().map(SalesOrderItem::getSkuCode).collect(Collectors.toList()));
        vo.setProductNames(items.stream().map(SalesOrderItem::getSkuName).collect(Collectors.toList()));

        // 查询出库状态
        OutboundOrder outbound = outboundOrderMapper.findByOrderId(order.getId());
        if (outbound != null) {
            vo.setOutboundStatus(outbound.getStatus());
        } else {
            vo.setOutboundStatus("NONE");
        }

        // 查询退货状态
        // 简化处理，实际应该查询退货单
        vo.setReturnStatus("NONE");

        return vo;
    }
}
