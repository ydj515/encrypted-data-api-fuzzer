package com.example.mockserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class PayloadCryptoAdapter {

    private final ObjectMapper objectMapper;

    public PayloadCryptoAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode decryptToJson(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            String plainJson = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readTree(plainJson);
        } catch (IllegalArgumentException | IOException ex) {
            throw new IllegalArgumentException("Invalid encrypted payload", ex);
        }
    }

    public <T> T decryptToObject(String encryptedData, Class<T> targetType) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            String plainJson = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readValue(plainJson, targetType);
        } catch (IllegalArgumentException | IOException ex) {
            throw new IllegalArgumentException("Invalid encrypted payload", ex);
        }
    }

    public Map<String, String> encryptResponse(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return Map.of("data", encoded);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encrypt response payload", ex);
        }
    }
}
