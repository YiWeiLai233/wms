package com.yiweilai.wms.privacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "privacy.crypto")
public class PrivacyCryptoProperties {

    private boolean enabled = true;

    private String algorithm = "AES_256_GCM";

    private String keyId = "v1";

    private String masterKey = "";

    private String hashKey = "";
}
