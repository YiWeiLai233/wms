package com.yiweilai.wms.stock.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockMapperXmlTest {

    @Test
    void stockQuerySelectsProductWarehouseDisplayFields() throws Exception {
        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/StockMapper.xml"),
                StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .contains("ps.sku_code AS sku_code")
                .contains("ps.name AS sku_name")
                .contains("w.name AS warehouse_name");
    }

}
