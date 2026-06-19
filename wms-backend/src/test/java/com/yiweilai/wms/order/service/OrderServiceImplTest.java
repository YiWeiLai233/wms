package com.yiweilai.wms.order.service;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.order.dto.OrderStatusUpdateDTO;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.order.service.impl.OrderServiceImpl;
import com.yiweilai.wms.privacy.crypto.PrivacyCryptoService;
import com.yiweilai.wms.privacy.crypto.PrivacyHashService;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.util.ProductImageHelper;
import com.yiweilai.wms.returns.mapper.ReturnOrderItemMapper;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private SalesOrderMapper orderMapper;
    @Mock
    private SalesOrderItemMapper orderItemMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private StockLogMapper stockLogMapper;
    @Mock
    private StockService stockService;
    @Mock
    private ProductImageHelper productImageHelper;
    @Mock
    private ReturnOrderItemMapper returnOrderItemMapper;
    @Mock
    private PrivacyCryptoService privacyCryptoService;
    @Mock
    private PrivacyHashService privacyHashService;
    @Mock
    private CacheService cacheService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orderMapper, orderItemMapper, productSkuMapper, productMapper,
                stockMapper, stockLogMapper, stockService, productImageHelper,
                returnOrderItemMapper, privacyCryptoService, privacyHashService, cacheService);
    }

    @Test
    void updateStatus_waitPayToWaitOutbound_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("WAIT_PAY");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("WAIT_OUTBOUND");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "WAIT_OUTBOUND");
        verify(cacheService).delete("cache:dashboard");
    }

    @Test
    void updateStatus_waitOutboundToOutbounding_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("WAIT_OUTBOUND");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("OUTBOUNDING");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "OUTBOUNDING");
    }

    @Test
    void updateStatus_outboundingToShipped_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("OUTBOUNDING");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("SHIPPED");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "SHIPPED");
        verify(orderMapper).updateShippedAt(1L);
    }

    @Test
    void updateStatus_shippedToExchanging_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("SHIPPED");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("EXCHANGING");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "EXCHANGING");
    }

    @Test
    void updateStatus_cancelFromAnyState_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderNo("SO001");
        order.setOrderStatus("SHIPPED");
        order.setWarehouseId(2L);

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("CANCELLED");

        when(orderMapper.findById(1L)).thenReturn(order);
        when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of());

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "CANCELLED");
    }

    @Test
    void updateStatus_invalidTransition_throwsException() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("WAIT_PAY");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("SHIPPED"); // WAIT_PAY -> SHIPPED 不允许

        when(orderMapper.findById(1L)).thenReturn(order);

        assertThatThrownBy(() -> service.updateStatus(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许从");
    }

    @Test
    void updateStatus_orderNotFound_throwsException() {
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(999L);
        dto.setTargetStatus("CANCELLED");

        when(orderMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStatus(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void updateStatus_shippedToFinished_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("SHIPPED");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("FINISHED");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "FINISHED");
    }

    @Test
    void updateStatus_exchangingToExchanged_success() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderStatus("EXCHANGING");

        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setOrderId(1L);
        dto.setTargetStatus("EXCHANGED");

        when(orderMapper.findById(1L)).thenReturn(order);

        service.updateStatus(dto);

        verify(orderMapper).updateStatus(1L, "EXCHANGED");
    }
}
