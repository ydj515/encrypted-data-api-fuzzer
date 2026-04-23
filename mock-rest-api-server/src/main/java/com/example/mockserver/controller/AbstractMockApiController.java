package com.example.mockserver.controller;

import com.example.mockserver.service.PayloadCryptoAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractMockApiController {

    protected final PayloadCryptoAdapter payloadCryptoAdapter;
    protected final ObjectMapper objectMapper;

    protected AbstractMockApiController(PayloadCryptoAdapter payloadCryptoAdapter,
                                        ObjectMapper objectMapper) {
        this.payloadCryptoAdapter = payloadCryptoAdapter;
        this.objectMapper = objectMapper;
    }

    protected boolean isEncryptedEnvelope(JsonNode body) {
        return body != null && body.hasNonNull("data") && body.get("data").isTextual();
    }

    protected String requiredText(JsonNode node, String fieldName) {
        String value = textValue(node, fieldName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    protected String textValue(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asText();
    }

    protected int intValue(JsonNode node, String fieldName, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asInt(defaultValue);
    }
}
