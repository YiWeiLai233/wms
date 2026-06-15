package com.yiweilai.wms.privacy.crypto;

import com.yiweilai.wms.privacy.config.PrivacyCryptoProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmPrivacyCryptoServiceTest {

    @Test
    void encryptUsesEnvelopeAndRandomIvThenDecryptsToPlaintext() {
        AesGcmPrivacyCryptoService service = new AesGcmPrivacyCryptoService(properties());

        String first = service.encrypt("Alice");
        String second = service.encrypt("Alice");

        assertThat(first).startsWith("ENC:v1:AES_256_GCM:");
        assertThat(second).startsWith("ENC:v1:AES_256_GCM:");
        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("Alice");
        assertThat(service.decrypt(second)).isEqualTo("Alice");
    }

    @Test
    void nullEmptyAndLegacyPlaintextAreCompatible() {
        AesGcmPrivacyCryptoService service = new AesGcmPrivacyCryptoService(properties());

        assertThat(service.encrypt(null)).isNull();
        assertThat(service.encrypt("")).isEmpty();
        assertThat(service.decrypt(null)).isNull();
        assertThat(service.decrypt("")).isEmpty();
        assertThat(service.decrypt("legacy plaintext")).isEqualTo("legacy plaintext");
        assertThat(service.isEncrypted("ENC:v1:AES_256_GCM:a:b")).isTrue();
        assertThat(service.isEncrypted("legacy plaintext")).isFalse();
    }

    @Test
    void decryptRejectsTamperedCiphertext() {
        AesGcmPrivacyCryptoService service = new AesGcmPrivacyCryptoService(properties());
        String encrypted = service.encrypt("Alice");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "xx";

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("decrypt");
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
