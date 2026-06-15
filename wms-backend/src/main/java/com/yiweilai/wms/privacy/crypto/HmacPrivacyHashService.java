package com.yiweilai.wms.privacy.crypto;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.privacy.config.PrivacyCryptoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class HmacPrivacyHashService implements PrivacyHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PrivacyCryptoProperties properties;

    @Override
    public String hmacSha256(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashKey(), HMAC_ALGORITHM));
            return toHex(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy hash failed");
        }
    }

    @Override
    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[\\s-]+", "");
        return normalized.isBlank() ? null : normalized;
    }

    @Override
    public String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private byte[] hashKey() {
        String hashKey = properties.getHashKey();
        if (!StringUtils.hasText(hashKey)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "privacy hash key is not configured");
        }
        return Base64.getDecoder().decode(hashKey);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
