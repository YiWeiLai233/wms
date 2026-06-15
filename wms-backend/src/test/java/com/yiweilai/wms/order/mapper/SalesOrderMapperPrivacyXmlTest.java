package com.yiweilai.wms.order.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SalesOrderMapperPrivacyXmlTest {

    @Test
    void mapperUsesHashColumnsForReceiverNameAndPhoneQueries() throws Exception {
        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/SalesOrderMapper.xml"),
                StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .contains("receiver_name_hash")
                .contains("receiver_phone_hash")
                .contains("so.receiver_name_hash = #{receiverNameHash}")
                .contains("so.receiver_phone_hash = #{receiverPhoneHash}")
                .doesNotContain("so.receiver_name LIKE")
                .doesNotContain("so.receiver_phone = #{receiverPhone}")
                .doesNotContain("so.receiver_phone LIKE");
    }
}
