package com.yiweilai.wms.product.service;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.product.dto.ProductSkuSaveDTO;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductBarcodeMapper;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.service.impl.ProductSkuServiceImpl;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.entity.StockLog;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import com.yiweilai.wms.warehouse.entity.WarehouseShelf;
import com.yiweilai.wms.warehouse.mapper.WarehouseShelfMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSkuServiceImplTest {

    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private ProductBarcodeMapper barcodeMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseShelfMapper shelfMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockLogMapper stockLogMapper;

    private ProductSkuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductSkuServiceImpl(
                skuMapper,
                barcodeMapper,
                productMapper,
                shelfMapper,
                stockMapper,
                stockLogMapper);
    }

    @Test
    void createSkuWithInitialInboundCreatesStockAndInboundLog() {
        ProductSkuSaveDTO dto = newSkuDto();
        ReflectionTestUtils.setField(dto, "initialQuantity", 12);
        ReflectionTestUtils.setField(dto, "warehouseId", 5L);
        ReflectionTestUtils.setField(dto, "shelfId", 20L);
        ReflectionTestUtils.setField(dto, "inboundRemark", "first batch");

        when(skuMapper.findBySkuCode("SKU-RED-42")).thenReturn(null);
        when(productMapper.findById(10L)).thenReturn(product(10L));
        when(shelfMapper.findById(20L)).thenReturn(shelf(20L, 5L));
        when(stockMapper.findBySkuAndWarehouse(77L, 5L)).thenReturn(null);
        when(skuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId(77L);
            return 1;
        });
        when(stockMapper.insert(any(Stock.class))).thenAnswer(invocation -> {
            Stock stock = invocation.getArgument(0);
            stock.setId(88L);
            return 1;
        });

        Long skuId = service.create(dto);

        assertThat(skuId).isEqualTo(77L);
        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockMapper).insert(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getSkuId()).isEqualTo(77L);
        assertThat(stockCaptor.getValue().getWarehouseId()).isEqualTo(5L);
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(12);
        assertThat(stockCaptor.getValue().getLockedQty()).isZero();
        assertThat(stockCaptor.getValue().getDefectiveQty()).isZero();

        ArgumentCaptor<StockLog> logCaptor = ArgumentCaptor.forClass(StockLog.class);
        verify(stockLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getBizType()).isEqualTo("INBOUND");
        assertThat(logCaptor.getValue().getBizNo()).isEqualTo("SKU_77");
        assertThat(logCaptor.getValue().getQuantityBefore()).isZero();
        assertThat(logCaptor.getValue().getQuantityChange()).isEqualTo(12);
        assertThat(logCaptor.getValue().getQuantityAfter()).isEqualTo(12);
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("first batch");
    }

    @Test
    void createSkuWithZeroInitialQuantityDoesNotWriteStockOrLog() {
        ProductSkuSaveDTO dto = newSkuDto();
        ReflectionTestUtils.setField(dto, "initialQuantity", 0);

        when(skuMapper.findBySkuCode("SKU-RED-42")).thenReturn(null);
        when(productMapper.findById(10L)).thenReturn(product(10L));
        when(skuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId(77L);
            return 1;
        });

        Long skuId = service.create(dto);

        assertThat(skuId).isEqualTo(77L);
        verify(stockMapper, never()).insert(any(Stock.class));
        verify(stockLogMapper, never()).insert(any(StockLog.class));
    }

    @Test
    void createSkuRejectsInitialInboundWhenShelfDoesNotBelongToWarehouse() {
        ProductSkuSaveDTO dto = newSkuDto();
        ReflectionTestUtils.setField(dto, "initialQuantity", 5);
        ReflectionTestUtils.setField(dto, "warehouseId", 5L);
        ReflectionTestUtils.setField(dto, "shelfId", 20L);

        when(skuMapper.findBySkuCode("SKU-RED-42")).thenReturn(null);
        when(productMapper.findById(10L)).thenReturn(product(10L));
        when(shelfMapper.findById(20L)).thenReturn(shelf(20L, 6L));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class);

        verify(skuMapper, never()).insert(any(ProductSku.class));
        verify(stockMapper, never()).insert(any(Stock.class));
        verify(stockLogMapper, never()).insert(any(StockLog.class));
    }

    private ProductSkuSaveDTO newSkuDto() {
        ProductSkuSaveDTO dto = new ProductSkuSaveDTO();
        dto.setProductId(10L);
        dto.setSkuCode("SKU-RED-42");
        dto.setName("Red 42");
        dto.setCostPrice(new BigDecimal("10.00"));
        dto.setSalePrice(new BigDecimal("19.90"));
        dto.setStatus(1);
        return dto;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private WarehouseShelf shelf(Long id, Long warehouseId) {
        WarehouseShelf shelf = new WarehouseShelf();
        shelf.setId(id);
        shelf.setWarehouseId(warehouseId);
        return shelf;
    }
}
