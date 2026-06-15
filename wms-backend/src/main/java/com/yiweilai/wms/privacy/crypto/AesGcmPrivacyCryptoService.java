package com.yiweilai.wms.privacy.crypto;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.privacy.config.PrivacyCryptoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AesGcmPrivacyCryptoService implements PrivacyCryptoService {

    private static final String ENCRYPTION_PREFIX = "ENC:";
    private static final String ALGORITHM_ID = "AES_256_GCM";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final PrivacyCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || isEncrypted(plaintext) || !properties.isEnabled()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return String.join(":",
                    "ENC",
                    normalizeKeyId(),
                    ALGORITHM_ID,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(cipherText));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy encrypt failed");
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty() || !isEncrypted(encryptedText) || !properties.isEnabled()) {
            return encryptedText;
        }
        try {
            String[] parts = encryptedText.split(":", 5);
            if (parts.length != 5 || !ALGORITHM_ID.equals(parts[2])) {
                throw new IllegalArgumentException("invalid privacy envelope");
            }

            byte[] iv = Base64.getDecoder().decode(parts[3]);
            byte[] cipherText = Base64.getDecoder().decode(parts[4]);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(cipherText);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy decrypt failed");
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTION_PREFIX);
    }

    private SecretKeySpec keySpec() {
        String masterKey = properties.getMasterKey();
        if (!StringUtils.hasText(masterKey)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy master key is not configured");
        }
        byte[] key = Base64.getDecoder().decode(masterKey);
        if (key.length != AES_256_KEY_BYTES) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy master key length must be 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }

    private String normalizeKeyId() {
        return StringUtils.hasText(properties.getKeyId()) ? properties.getKeyId().trim() : "v1";
    }
}
