package com.example.gateway.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Placeholder crypto implementation meant to be replaced with the institution's real algorithm.
 * Uses Base64 encode/decode so the gateway can run locally without external dependencies.
 */
@Component
public class Base64CryptoModule implements CryptoModule {

    private static final Logger log = LoggerFactory.getLogger(Base64CryptoModule.class);

    @Override
    public String encrypt(String key, String plainJson) {
        if (log.isDebugEnabled()) {
            log.debug("Encrypting payload with demo module for key prefix={}", key != null ? key.substring(0, Math.min(4, key.length())) : "null");
        }
        return Base64.getEncoder().encodeToString(plainJson.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String key, String encrypted) {
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
