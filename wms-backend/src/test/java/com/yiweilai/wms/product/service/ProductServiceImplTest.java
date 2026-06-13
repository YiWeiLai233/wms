package com.yiweilai.wms.product.service;

import com.yiweilai.wms.product.dto.ProductSaveDTO;
import com.yiweilai.wms.product.entity.Product;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductMapper;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.product.service.impl.ProductServiceImpl;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private WarehouseShelfMapper shelfMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockLogMapper stockLogMapper;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(
                productMapper,
                skuMapper,
                shelfMapper,
                stockMapper,
                stockLogMapper);
    }

    @Test
    void createProductCreatesSizeSkusAndInitialStockPerSize() {
        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setSpuCode("YZ503");
        dto.setName("豹纹");
        dto.setShelfId(20L);
        dto.setPrice(new BigDecimal("29.90"));
        dto.setStatus(1);
        dto.setSkuList(List.of(
                sizeSku("40", 7),
                sizeSku("41", 2)
        ));

        when(productMapper.findBySpuCode("YZ503")).thenReturn(null);
        when(shelfMapper.findById(20L)).thenReturn(shelf(20L, 5L));
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(10L);
            return 1;
        });
        when(skuMapper.findBySkuCode("YZ503-40")).thenReturn(null);
        when(skuMapper.findBySkuCode("YZ503-41")).thenReturn(null);
        when(skuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId("40".equals(sku.getSizeValue()) ? 101L : 102L);
            return 1;
        });

        Long productId = service.create(dto);

        assertThat(productId).isEqualTo(10L);

        ArgumentCaptor<ProductSku> skuCaptor = ArgumentCaptor.forClass(ProductSku.class);
        verify(skuMapper, times(2)).insert(skuCaptor.capture());
        assertThat(skuCaptor.getAllValues())
                .extracting(ProductSku::getSkuCode, ProductSku::getName, ProductSku::getSizeValue, ProductSku::getQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("YZ503-40", "豹纹-40", "40", 0),
                        org.assertj.core.groups.Tuple.tuple("YZ503-41", "豹纹-41", "41", 0));

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockMapper, times(2)).insert(stockCaptor.capture());
        assertThat(stockCaptor.getAllValues())
                .extracting(Stock::getSkuId, Stock::getWarehouseId, Stock::getQuantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, 5L, 7),
                        org.assertj.core.groups.Tuple.tuple(102L, 5L, 2));

        ArgumentCaptor<StockLog> logCaptor = ArgumentCaptor.forClass(StockLog.class);
        verify(stockLogMapper, times(2)).insert(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(StockLog::getBizType, StockLog::getQuantityChange, StockLog::getRemark)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("INBOUND", 7, "商品创建初始入库"),
                        org.assertj.core.groups.Tuple.tuple("INBOUND", 2, "商品创建初始入库"));
    }

    private ProductSaveDTO.SizeSkuDTO sizeSku(String size, int quantity) {
        ProductSaveDTO.SizeSkuDTO dto = new ProductSaveDTO.SizeSkuDTO();
        dto.setSizeValue(size);
        dto.setQuantity(quantity);
        dto.setCostPrice(new BigDecimal("10.00"));
        dto.setSalePrice(new BigDecimal("29.90"));
        dto.setWeight(new BigDecimal("0.50"));
        dto.setImage("shoe-" + size + ".png");
        return dto;
    }

    private WarehouseShelf shelf(Long id, Long warehouseId) {
        WarehouseShelf shelf = new WarehouseShelf();
        shelf.setId(id);
        shelf.setWarehouseId(warehouseId);
        return shelf;
    }
}
