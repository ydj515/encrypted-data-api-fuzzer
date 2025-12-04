package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.crypto.CryptoModule;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResponseBodyDecryptor {

    private static final Logger log = LoggerFactory.getLogger(ResponseBodyDecryptor.class);

    private final CryptoModule cryptoModule;

    public ResponseBodyDecryptor(CryptoModule cryptoModule) {
        this.cryptoModule = cryptoModule;
    }

    public String decrypt(ApiRoute route, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        try {
            String encryptedResponse = JsonPath.read(responseBody, "$.data");
            if (encryptedResponse == null) {
                return responseBody;
            }
            return cryptoModule.decrypt(route.key(), encryptedResponse);
        } catch (Exception ex) {
            log.warn("Failed to decrypt response body, returning raw upstream response: {}", ex.getMessage());
            return responseBody;
        }
    }
}
