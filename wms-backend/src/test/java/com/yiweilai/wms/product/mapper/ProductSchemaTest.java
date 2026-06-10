package com.yiweilai.wms.product.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSchemaTest {

    @Test
    void productTableDefinesColumnsUsedByProductMapper() throws Exception {
        String schema = Files.readString(Path.of("sql/schema.sql"), StandardCharsets.UTF_8);
        String mapper = Files.readString(Path.of("src/main/resources/mapper/ProductMapper.xml"), StandardCharsets.UTF_8);

        assertThat(mapper).contains("price");
        assertThat(productTableDefinition(schema)).contains("price");
    }

    private String productTableDefinition(String schema) {
        var matcher = Pattern.compile("CREATE TABLE IF NOT EXISTS product \\((.*?)\\) ENGINE=InnoDB", Pattern.DOTALL)
                .matcher(schema);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
