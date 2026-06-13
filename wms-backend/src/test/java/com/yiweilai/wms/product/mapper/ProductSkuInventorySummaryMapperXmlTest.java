package com.yiweilai.wms.product.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSkuInventorySummaryMapperXmlTest {

    @Test
    void skuListAggregatesInventorySummaryFromStockTable() throws Exception {
        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/ProductSkuMapper.xml"),
                StandardCharsets.UTF_8);
        String listVoSource = Files.readString(
                Path.of("src/main/java/com/yiweilai/wms/product/vo/ProductSkuListVO.java"),
                StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .contains("FROM stock")
                .contains("SUM(quantity) AS available_qty")
                .contains("SUM(locked_qty) AS locked_qty")
                .contains("SUM(defective_qty) AS defective_qty")
                .contains("AS total_qty")
                .contains("available_qty")
                .contains("total_qty");
        assertThat(listVoSource)
                .contains("availableQty")
                .contains("lockedQty")
                .contains("defectiveQty")
                .contains("totalQty");
    }
}
