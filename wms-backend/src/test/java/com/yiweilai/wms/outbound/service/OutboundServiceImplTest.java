package com.yiweilai.wms.outbound.service;

import com.yiweilai.wms.outbound.dto.OutboundBatchCreateDTO;
import com.yiweilai.wms.outbound.entity.OutboundOrder;
import com.yiweilai.wms.outbound.entity.OutboundOrderItem;
import com.yiweilai.wms.outbound.mapper.OutboundOrderItemMapper;
import com.yiweilai.wms.outbound.mapper.OutboundOrderMapper;
import com.yiweilai.wms.outbound.service.impl.OutboundServiceImpl;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.product.mapper.ProductBarcodeMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundServiceImplTest {

    @Mock
    private OutboundOrderMapper outboundOrderMapper;

    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private SalesOrderItemMapper salesOrderItemMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private ProductBarcodeMapper productBarcodeMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockLogMapper stockLogMapper;

    @Mock
    private WarehouseShelfMapper shelfMapper;

    @Mock
    private ExpressFeeStepMapper feeStepMapper;

    @Mock
    private ExpressFeeTemplateMapper feeTemplateMapper;

    private OutboundServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OutboundServiceImpl(
                outboundOrderMapper,
                outboundOrderItemMapper,
                salesOrderMapper,
                salesOrderItemMapper,
                productSkuMapper,
                productBarcodeMapper,
                stockMapper,
                stockLogMapper,
                shelfMapper,
                feeStepMapper,
                feeTemplateMapper);
    }

    @Test
    void createBatchCreatesOutboundOrdersForDistinctWaitOutboundOrders() {
        OutboundBatchCreateDTO dto = new OutboundBatchCreateDTO();
        dto.setOrderIds(List.of(1L, 2L, 1L));
        dto.setRemark("批量出库");

        when(salesOrderMapper.findById(1L)).thenReturn(order(1L, "SO001", 10L));
        when(salesOrderMapper.findById(2L)).thenReturn(order(2L, "SO002", 10L));
        when(outboundOrderMapper.findByOrderId(1L)).thenReturn(null);
        when(outboundOrderMapper.findByOrderId(2L)).thenReturn(null);
        when(salesOrderItemMapper.findByOrderId(1L)).thenReturn(List.of(orderItem(101L, "SKU-1", "鞋-1", 2)));
        when(salesOrderItemMapper.findByOrderId(2L)).thenReturn(List.of(orderItem(102L, "SKU-2", "鞋-2", 1)));

        AtomicLong idSequence = new AtomicLong(100L);
        when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenAnswer(invocation -> {
            OutboundOrder outboundOrder = invocation.getArgument(0);
            outboundOrder.setId(idSequence.incrementAndGet());
            return 1;
        });

        List<Long> outboundIds = service.createBatch(dto);

        assertThat(outboundIds).containsExactly(101L, 102L);

        ArgumentCaptor<OutboundOrder> outboundCaptor = ArgumentCaptor.forClass(OutboundOrder.class);
        verify(outboundOrderMapper, times(2)).insert(outboundCaptor.capture());
        assertThat(outboundCaptor.getAllValues())
                .extracting(OutboundOrder::getOrderId, OutboundOrder::getOrderNo,
                        OutboundOrder::getWarehouseId, OutboundOrder::getStatus, OutboundOrder::getRemark)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "SO001", 10L, "WAIT_PICKING", "批量出库"),
                        org.assertj.core.groups.Tuple.tuple(2L, "SO002", 10L, "WAIT_PICKING", "批量出库"));
        assertThat(outboundCaptor.getAllValues())
                .extracting(OutboundOrder::getOutboundNo)
                .doesNotHaveDuplicates();

        ArgumentCaptor<OutboundOrderItem> itemCaptor = ArgumentCaptor.forClass(OutboundOrderItem.class);
        verify(outboundOrderItemMapper, times(2)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues())
                .extracting(OutboundOrderItem::getOutboundId, OutboundOrderItem::getSkuId,
                        OutboundOrderItem::getSkuCode, OutboundOrderItem::getQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, 101L, "SKU-1", 2),
                        org.assertj.core.groups.Tuple.tuple(102L, 102L, "SKU-2", 1));

        verify(salesOrderMapper).updateStatus(1L, "OUTBOUNDING");
        verify(salesOrderMapper).updateStatus(2L, "OUTBOUNDING");
    }

    private SalesOrder order(Long id, String orderNo, Long warehouseId) {
        SalesOrder order = new SalesOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setWarehouseId(warehouseId);
        order.setOrderStatus("WAIT_OUTBOUND");
        return order;
    }

    private SalesOrderItem orderItem(Long skuId, String skuCode, String skuName, Integer quantity) {
        SalesOrderItem item = new SalesOrderItem();
        item.setSkuId(skuId);
        item.setSkuCode(skuCode);
        item.setSkuName(skuName);
        item.setQuantity(quantity);
        return item;
    }
}
