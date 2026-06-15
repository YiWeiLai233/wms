package com.yiweilai.wms.privacy.crypto;

public interface PrivacyHashService {

    String hmacSha256(String plaintext);

    String normalizePhone(String phone);

    String normalizeName(String name);
}
