package com.yiweilai.wms.order.service;

import com.yiweilai.wms.order.dto.OrderImportDTO;
import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.entity.SalesOrderItem;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.order.service.impl.OrderServiceImpl;
import com.yiweilai.wms.order.vo.OrderVO;
import com.yiweilai.wms.privacy.config.PrivacyCryptoProperties;
import com.yiweilai.wms.privacy.crypto.AesGcmPrivacyCryptoService;
import com.yiweilai.wms.privacy.crypto.HmacPrivacyHashService;
import com.yiweilai.wms.product.entity.ProductSku;
import com.yiweilai.wms.product.mapper.ProductSkuMapper;
import com.yiweilai.wms.stock.entity.Stock;
import com.yiweilai.wms.stock.mapper.StockLogMapper;
import com.yiweilai.wms.stock.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServicePrivacyTest {

    private SalesOrderMapper orderMapper;
    private SalesOrderItemMapper orderItemMapper;
    private ProductSkuMapper productSkuMapper;
    private StockMapper stockMapper;
    private StockLogMapper stockLogMapper;
    private AesGcmPrivacyCryptoService cryptoService;
    private HmacPrivacyHashService hashService;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(SalesOrderMapper.class);
        orderItemMapper = mock(SalesOrderItemMapper.class);
        productSkuMapper = mock(ProductSkuMapper.class);
        stockMapper = mock(StockMapper.class);
        stockLogMapper = mock(StockLogMapper.class);
        cryptoService = new AesGcmPrivacyCryptoService(properties());
        hashService = new HmacPrivacyHashService(properties());
        service = new OrderServiceImpl(
                orderMapper,
                orderItemMapper,
                productSkuMapper,
                stockMapper,
                stockLogMapper,
                cryptoService,
                hashService);
    }

    @Test
    void importOrderEncryptsReceiverFieldsAndWritesLookupHashes() {
        when(productSkuMapper.findBySkuCode("SKU-1")).thenReturn(sku());
        when(orderMapper.insert(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        });
        when(stockMapper.findAvailableBySkuAndWarehouse(10L, 2L))
                .thenReturn(List.of(stock()));
        when(stockMapper.deductQuantity(20L, 1)).thenReturn(1);

        Long orderId = service.importOrder(importDto());

        assertThat(orderId).isEqualTo(101L);
        ArgumentCaptor<SalesOrder> orderCaptor = ArgumentCaptor.forClass(SalesOrder.class);
        org.mockito.Mockito.verify(orderMapper).insert(orderCaptor.capture());
        SalesOrder inserted = orderCaptor.getValue();
        assertThat(inserted.getReceiverName()).startsWith("ENC:v1:AES_256_GCM:");
        assertThat(inserted.getReceiverPhone()).startsWith("ENC:v1:AES_256_GCM:");
        assertThat(inserted.getReceiverAddress()).startsWith("ENC:v1:AES_256_GCM:");
        assertThat(cryptoService.decrypt(inserted.getReceiverName())).isEqualTo("Alice");
        assertThat(cryptoService.decrypt(inserted.getReceiverPhone())).isEqualTo("138 0013-8000");
        assertThat(cryptoService.decrypt(inserted.getReceiverAddress())).isEqualTo("Address 1");
        assertThat(inserted.getReceiverNameHash())
                .isEqualTo(hashService.hmacSha256(hashService.normalizeName("Alice")));
        assertThat(inserted.getReceiverPhoneHash())
                .isEqualTo(hashService.hmacSha256(hashService.normalizePhone("138 0013-8000")));
    }

    @Test
    void getByIdDecryptsEncryptedReceiverFieldsForFrontendVo() {
        SalesOrder encrypted = new SalesOrder();
        encrypted.setId(101L);
        encrypted.setOrderNo("SO-1");
        encrypted.setReceiverName(cryptoService.encrypt("Alice"));
        encrypted.setReceiverPhone(cryptoService.encrypt("13800138000"));
        encrypted.setReceiverAddress(cryptoService.encrypt("Address 1"));
        when(orderMapper.findById(101L)).thenReturn(encrypted);
        when(orderItemMapper.findByOrderId(101L)).thenReturn(List.of());

        OrderVO vo = service.getById(101L);

        assertThat(vo.getReceiverName()).isEqualTo("Alice");
        assertThat(vo.getReceiverPhone()).isEqualTo("13800138000");
        assertThat(vo.getReceiverAddress()).isEqualTo("Address 1");
    }

    private OrderImportDTO importDto() {
        OrderImportDTO dto = new OrderImportDTO();
        dto.setWarehouseId(2L);
        dto.setPlatformOrderNo("P-1");
        dto.setReceiverName("Alice");
        dto.setReceiverPhone("138 0013-8000");
        dto.setReceiverAddress("Address 1");
        OrderImportDTO.OrderItemDTO item = new OrderImportDTO.OrderItemDTO();
        item.setSkuCode("SKU-1");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10.00"));
        dto.setItems(List.of(item));
        return dto;
    }

    private ProductSku sku() {
        ProductSku sku = new ProductSku();
        sku.setId(10L);
        sku.setSkuCode("SKU-1");
        sku.setName("Product 1");
        return sku;
    }

    private Stock stock() {
        Stock stock = new Stock();
        stock.setId(20L);
        stock.setQuantity(5);
        return stock;
    }

    private PrivacyCryptoProperties properties() {
        PrivacyCryptoProperties properties = new PrivacyCryptoProperties();
        properties.setEnabled(true);
        properties.setAlgorithm("AES_256_GCM");
        properties.setKeyId("v1");
        properties.setMasterKey(base64("0123456789abcdef0123456789abcdef"));
        properties.setHashKey(base64("abcdef0123456789abcdef0123456789"));
        return properties;
    }

    private String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
