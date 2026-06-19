package com.yiweilai.wms.returns.service;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.util.ProductImageHelper;
import com.yiweilai.wms.returns.entity.ReturnOrder;
import com.yiweilai.wms.returns.mapper.ReturnOrderItemMapper;
import com.yiweilai.wms.returns.mapper.ReturnOrderMapper;
import com.yiweilai.wms.returns.service.impl.ReturnServiceImpl;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceImplTest {

    @Mock
    private ReturnOrderMapper returnOrderMapper;
    @Mock
    private ReturnOrderItemMapper returnOrderItemMapper;
    @Mock
    private SalesOrderMapper salesOrderMapper;
    @Mock
    private SalesOrderItemMapper salesOrderItemMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageHelper productImageHelper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private StockLogMapper stockLogMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private ExpressFeeStepMapper feeStepMapper;
    @Mock
    private ExpressFeeTemplateMapper feeTemplateMapper;
    @Mock
    private CacheService cacheService;

    private ReturnServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReturnServiceImpl(
                returnOrderMapper, returnOrderItemMapper, salesOrderMapper,
                salesOrderItemMapper, productSkuMapper, productMapper,
                productImageHelper, stockMapper, stockLogMapper, warehouseMapper,
                feeStepMapper, feeTemplateMapper, cacheService);
    }

    @Test
    void cancel_pendingCheckStatus_success() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setId(1L);
        returnOrder.setOrderId(10L);
        returnOrder.setStatus("PENDING_CHECK");

        when(returnOrderMapper.findById(1L)).thenReturn(returnOrder);
        when(returnOrderMapper.findByOrderId(10L)).thenReturn(List.of(returnOrder));

        service.cancel(1L);

        verify(returnOrderMapper).updateStatus(1L, "CANCELLED");
    }

    @Test
    void cancel_sellableStatus_success() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setId(1L);
        returnOrder.setOrderId(10L);
        returnOrder.setStatus("SELLABLE");

        when(returnOrderMapper.findById(1L)).thenReturn(returnOrder);
        when(returnOrderMapper.findByOrderId(10L)).thenReturn(List.of(returnOrder));

        service.cancel(1L);

        verify(returnOrderMapper).updateStatus(1L, "CANCELLED");
    }

    @Test
    void cancel_completedStatus_throwsException() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setId(1L);
        returnOrder.setStatus("COMPLETED");

        when(returnOrderMapper.findById(1L)).thenReturn(returnOrder);

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不允许取消退货");
    }

    @Test
    void cancel_returnNotFound_throwsException() {
        when(returnOrderMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("退货单不存在");
    }

    @Test
    void cancelByOrderId_hasReturnOrder_success() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setId(1L);
        returnOrder.setOrderId(10L);
        returnOrder.setStatus("PENDING_CHECK");

        when(returnOrderMapper.findLatestByOrderId(10L)).thenReturn(returnOrder);
        when(returnOrderMapper.findByOrderId(10L)).thenReturn(List.of(returnOrder));

        service.cancelByOrderId(10L);

        verify(returnOrderMapper).updateStatus(1L, "CANCELLED");
    }

    @Test
    void cancelByOrderId_noReturnOrder_throwsException() {
        when(returnOrderMapper.findLatestByOrderId(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelByOrderId(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该订单没有退货单");
    }
}
