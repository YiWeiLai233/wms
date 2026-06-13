package com.yiweilai.wms.outbound.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundScanQuantityGuardTest {

    @Test
    void confirmOutboundNoLongerRequiresPickedQuantityOrShelfSelection() throws Exception {
        String serviceSource = Files.readString(
                Path.of("src/main/java/com/yiweilai/wms/outbound/service/impl/OutboundServiceImpl.java"),
                StandardCharsets.UTF_8);

        assertThat(serviceSource)
                .doesNotContain("pickedQty < item.getQuantity()")
                .doesNotContain("商品未选择出库货架")
                .contains("findAvailableBySkuAndWarehouse");
    }
}
