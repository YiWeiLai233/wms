package com.yiweilai.wms.warehouse.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseDisplayMapperXmlTest {

    @Test
    void businessOrderQueriesSelectWarehouseName() throws Exception {
        assertMapperSelectsWarehouseName("src/main/resources/mapper/SalesOrderMapper.xml");
        assertMapperSelectsWarehouseName("src/main/resources/mapper/OutboundOrderMapper.xml");
        assertMapperSelectsWarehouseName("src/main/resources/mapper/ReturnOrderMapper.xml");
    }

    private void assertMapperSelectsWarehouseName(String mapperPath) throws Exception {
        String mapperXml = Files.readString(Path.of(mapperPath), StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .describedAs(mapperPath)
                .contains("w.name AS warehouse_name");
    }
}
