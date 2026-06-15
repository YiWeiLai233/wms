package com.yiweilai.wms.privacy.crypto;

import com.yiweilai.wms.privacy.config.PrivacyCryptoProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class HmacPrivacyHashServiceTest {

    @Test
    void phoneHashNormalizesSpacesAndDashes() {
        HmacPrivacyHashService service = new HmacPrivacyHashService(properties());

        String compact = service.hmacSha256(service.normalizePhone("13800138000"));
        String formatted = service.hmacSha256(service.normalizePhone("138 0013-8000"));

        assertThat(formatted).isEqualTo(compact);
        assertThat(formatted).hasSize(64);
        assertThat(formatted).matches("[0-9a-f]{64}");
    }

    @Test
    void blankValuesNormalizeToNull() {
        HmacPrivacyHashService service = new HmacPrivacyHashService(properties());

        assertThat(service.normalizePhone("   -  ")).isNull();
        assertThat(service.normalizeName("   ")).isNull();
        assertThat(service.hmacSha256(null)).isNull();
        assertThat(service.hmacSha256("")).isNull();
    }

    @Test
    void nameHashTrimsBeforeHmac() {
        HmacPrivacyHashService service = new HmacPrivacyHashService(properties());

        assertThat(service.hmacSha256(service.normalizeName(" Alice ")))
                .isEqualTo(service.hmacSha256(service.normalizeName("Alice")));
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
