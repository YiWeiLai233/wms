package com.yiweilai.wms.stock.service;

import com.yiweilai.wms.alert.service.StockAlertConfigService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockCheckItemMapper;
import com.yiweilai.wms.stock.mapper.StockCheckMapper;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.stock.service.impl.StockServiceImpl;
import com.yiweilai.wms.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockMapper stockMapper;
    @Mock
    private StockLogMapper stockLogMapper;
    @Mock
    private StockCheckMapper stockCheckMapper;
    @Mock
    private StockCheckItemMapper stockCheckItemMapper;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private StockAlertConfigService stockAlertConfigService;

    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StockServiceImpl(
                stockMapper, stockLogMapper, stockCheckMapper,
                stockCheckItemMapper, warehouseMapper, stockAlertConfigService);
    }

    @Test
    void deductStock_success() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSkuId(10L);
        stock.setWarehouseId(2L);
        stock.setQuantity(10);

        when(stockMapper.findAvailableBySkuAndWarehouse(10L, 2L)).thenReturn(List.of(stock));
        when(stockMapper.deductQuantity(1L, 5)).thenReturn(1);
        when(stockMapper.findById(1L)).thenAnswer(inv -> {
            Stock updated = new Stock();
            updated.setId(1L);
            updated.setQuantity(5);
            return updated;
        });

        service.deductStock(10L, 5, "ORD001", 2L, "OUTBOUND", "出库扣减");

        verify(stockMapper).deductQuantity(1L, 5);
        verify(stockLogMapper).insert(any(StockLog.class));
    }

    @Test
    void deductStock_insufficientStock_throwsException() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setQuantity(3);

        when(stockMapper.findAvailableBySkuAndWarehouse(10L, 2L)).thenReturn(List.of(stock));
        // deductQuantity 会尝试扣减3（Math.min(3, 5)），返回0表示失败
        when(stockMapper.deductQuantity(1L, 3)).thenReturn(0);

        assertThatThrownBy(() -> service.deductStock(10L, 5, "ORD001", 2L, "OUTBOUND", "出库扣减"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void deductStock_noStockRecord_throwsException() {
        when(stockMapper.findAvailableBySkuAndWarehouse(10L, 2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.deductStock(10L, 5, "ORD001", 2L, "OUTBOUND", "出库扣减"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void addStock_existingStock_increasesQuantity() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSkuId(10L);
        stock.setWarehouseId(2L);
        stock.setQuantity(10);

        when(stockMapper.findBySkuAndWarehouse(10L, 2L)).thenReturn(stock);

        service.addStock(10L, 5, "RET001", 2L, "RETURN", "退货入库");

        verify(stockMapper).addQuantity(1L, 5);
        verify(stockLogMapper).insert(any(StockLog.class));
    }

    @Test
    void addStock_newStock_createsRecord() {
        when(stockMapper.findBySkuAndWarehouse(10L, 2L)).thenReturn(null);
        when(stockMapper.insert(any(Stock.class))).thenAnswer(inv -> {
            Stock s = inv.getArgument(0);
            s.setId(99L);
            return 1;
        });

        service.addStock(10L, 5, "RET001", 2L, "RETURN", "退货入库");

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockMapper).insert(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(5);
        verify(stockLogMapper).insert(any(StockLog.class));
    }

    @Test
    void adjust_positiveQuantity_increasesStock() {
        StockAdjustDTO dto = new StockAdjustDTO();
        dto.setSkuId(10L);
        dto.setWarehouseId(2L);
        dto.setQuantity(5);
        dto.setRemark("入库调整");

        when(stockMapper.findBySkuAndWarehouse(10L, 2L)).thenReturn(null);
        when(stockMapper.insert(any(Stock.class))).thenAnswer(inv -> {
            Stock s = inv.getArgument(0);
            s.setId(99L);
            return 1;
        });

        service.adjust(dto);

        verify(stockMapper).insert(any(Stock.class));
        verify(stockLogMapper).insert(any(StockLog.class));
    }

    @Test
    void adjust_negativeQuantity_decreasesStock() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setQuantity(10);

        StockAdjustDTO dto = new StockAdjustDTO();
        dto.setSkuId(10L);
        dto.setWarehouseId(2L);
        dto.setQuantity(-3);
        dto.setRemark("出库调整");

        when(stockMapper.findBySkuAndWarehouse(10L, 2L)).thenReturn(stock);

        service.adjust(dto);

        // adjust 方法使用 updateQuantity 而不是 deductQuantity
        verify(stockMapper).updateQuantity(1L, 7); // 10 + (-3) = 7
        verify(stockLogMapper).insert(any(StockLog.class));
    }
}
