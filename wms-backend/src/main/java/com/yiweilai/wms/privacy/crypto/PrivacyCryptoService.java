package com.yiweilai.wms.privacy.crypto;

public interface PrivacyCryptoService {

    String encrypt(String plaintext);

    String decrypt(String encryptedText);

    boolean isEncrypted(String value);
}
