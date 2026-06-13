package com.yiweilai.wms.stock.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockLogMapperXmlTest {

    @Test
    void stockLogQueryExposesPlatformOrderAndTimeRangeFilters() throws Exception {
        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/StockLogMapper.xml"),
                StandardCharsets.UTF_8);
        String queryDto = Files.readString(
                Path.of("src/main/java/com/yiweilai/wms/stock/dto/StockLogQueryDTO.java"),
                StandardCharsets.UTF_8);
        String vo = Files.readString(
                Path.of("src/main/java/com/yiweilai/wms/stock/vo/StockLogVO.java"),
                StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .contains("platform_order_no")
                .contains("platformOrderNo")
                .contains("startTime")
                .contains("endTime")
                .contains("LEFT JOIN product_sku")
                .contains("LEFT JOIN warehouse ");
        assertThat(queryDto)
                .contains("platformOrderNo")
                .contains("startTime")
                .contains("endTime");
        assertThat(vo).contains("platformOrderNo");
    }
}
