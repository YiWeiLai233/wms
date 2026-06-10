package com.yiweilai.wms.product.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSaveDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesNumericCategoryIdWithJackson3() throws Exception {
        String json = """
                {
                  "spuCode": "SPU-001",
                  "name": "测试商品",
                  "categoryId": 3
                }
                """;

        ProductSaveDTO dto = objectMapper.readValue(json, ProductSaveDTO.class);

        assertThat(dto.getFinalCategoryId()).isEqualTo(3L);
    }

    @Test
    void deserializesCascaderCategoryIdWithJackson3() throws Exception {
        String json = """
                {
                  "spuCode": "SPU-002",
                  "name": "测试商品",
                  "categoryId": [1, 2, 3]
                }
                """;

        ProductSaveDTO dto = objectMapper.readValue(json, ProductSaveDTO.class);

        assertThat(dto.getFinalCategoryId()).isEqualTo(3L);
    }
}
