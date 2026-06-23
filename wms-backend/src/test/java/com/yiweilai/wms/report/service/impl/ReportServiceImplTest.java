package com.yiweilai.wms.report.service.impl;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.stock.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private CacheService cacheService;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(jdbcTemplate, stockMapper, cacheService);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void expressFeeReportCountsOriginalOutboundFeeSeparatelyFromExchangeOutboundFee() {
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("total_fee", BigDecimal.ZERO, "total_count", 0L));
        doReturn(List.of())
                .when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        service.getExpressFeeReport(null, null, null, null, null);

        ArgumentCaptor<String> summarySqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).queryForMap(summarySqlCaptor.capture(), any(Object[].class));

        String outboundSummarySql = summarySqlCaptor.getAllValues().get(0);
        assertThat(outboundSummarySql)
                .contains("oo.status IN ('SHIPPED','EXCHANGED')")
                .contains("oo.remark NOT LIKE '换货单[%自动创建%'");

        ArgumentCaptor<String> detailSqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).query(detailSqlCaptor.capture(), any(RowMapper.class), any(Object[].class));

        String outboundDetailSql = detailSqlCaptor.getAllValues().get(0);
        assertThat(outboundDetailSql)
                .contains("oo.status IN ('SHIPPED','EXCHANGED')")
                .contains("oo.remark NOT LIKE '换货单[%自动创建%'");
    }

    @Test
    void updateExpressFeeItemUpdatesOutboundFeeAndCompany() {
        service.updateExpressFeeItem("OUTBOUND", 10L, 3L, new BigDecimal("12.50"));

        verify(jdbcTemplate).update(
                "UPDATE outbound_order SET express_company_id = ?, shipping_fee = ? WHERE id = ? AND deleted = 0",
                3L,
                new BigDecimal("12.50"),
                10L);
    }

    @Test
    void updateExpressFeeItemUpdatesExchangeFeeAndCompany() {
        service.updateExpressFeeItem("EXCHANGE", 10L, 3L, new BigDecimal("12.50"));

        verify(jdbcTemplate).update(
                "UPDATE exchange_order SET express_company_id = ?, shipping_fee = ? WHERE id = ? AND deleted = 0",
                3L,
                new BigDecimal("12.50"),
                10L);
    }

    @Test
    void deleteExpressFeeItemClearsOutboundFeeAndCompany() {
        service.deleteExpressFeeItem("OUTBOUND", 10L);

        verify(jdbcTemplate).update(
                "UPDATE outbound_order SET express_company_id = NULL, shipping_fee = NULL WHERE id = ? AND deleted = 0",
                10L);
    }

    @Test
    void expressFeeWriteRejectsReturnRowsBecauseTheyHaveNoCompanyField() {
        assertThatThrownBy(() -> service.updateExpressFeeItem("RETURN", 10L, 3L, new BigDecimal("12.50")))
                .hasMessageContaining("仅支持修改出库和换货快递费用");

        assertThatThrownBy(() -> service.deleteExpressFeeItem("RETURN", 10L))
                .hasMessageContaining("仅支持删除出库和换货快递费用");
    }
}
