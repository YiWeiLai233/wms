package com.yiweilai.wms.exchange.service;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exchange.entity.ExchangeOrder;
import com.yiweilai.wms.exchange.entity.ExchangeOrderItem;
import com.yiweilai.wms.exchange.mapper.ExchangeOrderItemMapper;
import com.yiweilai.wms.exchange.mapper.ExchangeOrderMapper;
import com.yiweilai.wms.exchange.service.impl.ExchangeServiceImpl;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.outbound.entity.OutboundOrder;
import com.yiweilai.wms.outbound.mapper.OutboundOrderItemMapper;
import com.yiweilai.wms.outbound.mapper.OutboundOrderMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceImplTest {

    @Mock
    private ExchangeOrderMapper exchangeOrderMapper;

    @Mock
    private ExchangeOrderItemMapper exchangeOrderItemMapper;

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private SalesOrderItemMapper salesOrderItemMapper;

    @Mock
    private OutboundOrderMapper outboundOrderMapper;

    @Mock
    private OutboundOrderItemMapper outboundOrderItemMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockLogMapper stockLogMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private CacheService cacheService;

    private ExchangeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExchangeServiceImpl(
                exchangeOrderMapper,
                exchangeOrderItemMapper,
                salesOrderMapper,
                salesOrderItemMapper,
                outboundOrderMapper,
                outboundOrderItemMapper,
                stockMapper,
                stockLogMapper,
                warehouseMapper,
                cacheService);
    }

    @Test
    void shipRetiresOriginalOutboundAndMarksReplacementOutboundAsShipped() {
        BigDecimal shippingFee = new BigDecimal("8.50");
        ExchangeOrder exchangeOrder = exchangeOrder();
        ExchangeOrderItem returnedItem = exchangeItem(1L, 10L, "OLD-SKU", "Old product", "RETURN_ITEM");
        ExchangeOrderItem replacementItem = exchangeItem(2L, 20L, "NEW-SKU", "New product", "EXCHANGE_ITEM");
        Stock replacementStock = stock(30L, 5);
        OutboundOrder originalOutbound = new OutboundOrder();
        originalOutbound.setId(100L);
        SalesOrderItem oldOrderItem = new SalesOrderItem();
        oldOrderItem.setId(200L);
        oldOrderItem.setSkuId(10L);

        when(exchangeOrderMapper.findById(9L)).thenReturn(exchangeOrder);
        when(exchangeOrderItemMapper.findByExchangeId(9L)).thenReturn(List.of(returnedItem, replacementItem));
        when(outboundOrderMapper.insert(any(OutboundOrder.class))).thenAnswer(invocation -> {
            OutboundOrder outboundOrder = invocation.getArgument(0);
            outboundOrder.setId(777L);
            return 1;
        });
        when(stockMapper.findBySkuAndWarehouse(20L, 5L)).thenReturn(replacementStock);
        when(outboundOrderMapper.findByOrderIdAndStatus(11L, "SHIPPED")).thenReturn(originalOutbound);
        when(salesOrderItemMapper.findByOrderId(11L)).thenReturn(List.of(oldOrderItem));

        service.ship(9L, 3L, "TRACK-NEW", shippingFee);

        ArgumentCaptor<OutboundOrder> outboundCaptor = ArgumentCaptor.forClass(OutboundOrder.class);
        verify(outboundOrderMapper).insert(outboundCaptor.capture());
        assertThat(outboundCaptor.getValue())
                .extracting(OutboundOrder::getOrderId, OutboundOrder::getOrderNo, OutboundOrder::getWarehouseId)
                .containsExactly(11L, "SO001", 5L);

        verify(outboundOrderMapper).updateExpressInfo(777L, 3L, "TRACK-NEW", shippingFee);
        verify(outboundOrderMapper).updateStatus(777L, "SHIPPED");
        verify(outboundOrderMapper).updateShippedAt(777L);

        InOrder outboundOrder = inOrder(outboundOrderMapper);
        outboundOrder.verify(outboundOrderMapper).findByOrderIdAndStatus(11L, "SHIPPED");
        outboundOrder.verify(outboundOrderMapper).updateStatus(100L, "EXCHANGED");
        outboundOrder.verify(outboundOrderMapper).updateStatus(777L, "SHIPPED");
    }

    private ExchangeOrder exchangeOrder() {
        ExchangeOrder order = new ExchangeOrder();
        order.setId(9L);
        order.setExchangeNo("EX001");
        order.setOrderId(11L);
        order.setOrderNo("SO001");
        order.setWarehouseId(5L);
        order.setStatus("CHECKED");
        return order;
    }

    private ExchangeOrderItem exchangeItem(Long id, Long skuId, String skuCode, String skuName, String itemType) {
        ExchangeOrderItem item = new ExchangeOrderItem();
        item.setId(id);
        item.setExchangeId(9L);
        item.setSkuId(skuId);
        item.setSkuCode(skuCode);
        item.setSkuName(skuName);
        item.setQuantity(1);
        item.setItemType(itemType);
        return item;
    }

    private Stock stock(Long id, int quantity) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setQuantity(quantity);
        return stock;
    }
}
