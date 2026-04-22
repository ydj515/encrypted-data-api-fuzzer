package com.example.gateway.service;

import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
import com.example.gateway.config.ApiRoute;
import com.example.gateway.util.RouteUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public final class GatewayProxyService {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    private final CryptoModule cryptoModule;
    private final ChecksumModule checksumModule;
    private final RouteResolver routeResolver;
    private final GatewayHeaderFactory gatewayHeaderFactory;
    private final UpstreamClient upstreamClient;
    private final ResponseBodyDecryptor responseBodyDecryptor;
    private final ObjectMapper objectMapper;

    public GatewayProxyService(CryptoModule cryptoModule,
                               ChecksumModule checksumModule,
                               RouteResolver routeResolver,
                               GatewayHeaderFactory gatewayHeaderFactory,
                               UpstreamClient upstreamClient,
                               ResponseBodyDecryptor responseBodyDecryptor,
                               ObjectMapper objectMapper) {
        this.cryptoModule = cryptoModule;
        this.checksumModule = checksumModule;
        this.routeResolver = routeResolver;
        this.gatewayHeaderFactory = gatewayHeaderFactory;
        this.upstreamClient = upstreamClient;
        this.responseBodyDecryptor = responseBodyDecryptor;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> proxyPost(String org, String service, String api, String plainBody) {
        validatePathSegment("org", org);
        validatePathSegment("service", service);
        validatePathSegment("api", api);

        ApiRoute route = routeResolver.resolve(org, service, api, "POST");
        URI targetUri = buildTargetUri(route, plainBody);

        if (route.isEncrypted()) {
            String encryptedData = cryptoModule.encrypt(route.key(), plainBody);
            String checksum = checksumModule.checksum(encryptedData);
            var headers = gatewayHeaderFactory.build(route, checksum);
            ResponseEntity<String> upstream = upstreamClient.post(targetUri, headers, Map.of("data", encryptedData));
            return ResponseEntity.status(upstream.getStatusCode())
                    .body(responseBodyDecryptor.decrypt(route, upstream.getBody()));
        }

        var headers = gatewayHeaderFactory.build(route, null);
        ResponseEntity<String> upstream = upstreamClient.post(targetUri, headers, parseBody(plainBody));
        return ResponseEntity.status(upstream.getStatusCode()).body(upstream.getBody());
    }

    private URI buildTargetUri(ApiRoute route, String plainBody) {
        if (!RouteUtils.hasTemplateVariables(route.externalPath())) {
            return RouteUtils.buildTargetUri(route);
        }

        Set<String> requiredVariables = RouteUtils.extractTemplateVariables(route.externalPath());
        Map<String, String> variableValues = parseTemplateVariables(plainBody, requiredVariables);
        return RouteUtils.buildTargetUri(route, variableValues);
    }

    private Map<String, Object> parseBody(String plainBody) {
        try {
            return objectMapper.readValue(plainBody, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid JSON body", ex);
        }
    }

    private Map<String, String> parseTemplateVariables(String plainBody, Set<String> requiredVariables) {
        Map<String, Object> payload = parseBody(plainBody);
        Map<String, String> variables = new HashMap<>();

        for (String variable : requiredVariables) {
            Object directValue = payload.get(variable);
            if (directValue != null) {
                String value = String.valueOf(directValue);
                validatePathVariable(variable, value);
                variables.put(variable, value);
                continue;
            }

            Object pathVariables = payload.get("pathVariables");
            if (pathVariables instanceof Map<?, ?> nested) {
                Object nestedValue = nested.get(variable);
                if (nestedValue != null) {
                    String value = String.valueOf(nestedValue);
                    validatePathVariable(variable, value);
                    variables.put(variable, value);
                }
            }
        }
        return variables;
    }

    private void validatePathVariable(String variableName, String value) {
        if (!PATH_VARIABLE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid path variable format: " + variableName);
        }
    }

    private void validatePathSegment(String segmentName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid path segment: " + segmentName);
        }
        if (!PATH_VARIABLE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid path segment format: " + segmentName);
        }
    }
}
