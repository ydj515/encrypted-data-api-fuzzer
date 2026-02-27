package com.example.gateway.service;

import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
import com.example.gateway.config.ApiRoute;
import com.example.gateway.util.RouteUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public final class GatewayProxyService {

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
        ApiRoute route = routeResolver.resolve(org, service, api, "POST");

        String encryptedData = cryptoModule.encrypt(route.key(), plainBody);
        String checksum = checksumModule.checksum(encryptedData);

        URI targetUri = buildTargetUri(route, plainBody);
        var headers = gatewayHeaderFactory.build(route, checksum);

        Map<String, Object> requestBody = Map.of("data", encryptedData);

        ResponseEntity<String> upstreamResponse = upstreamClient.post(targetUri, headers, requestBody);
        String decryptedBody = responseBodyDecryptor.decrypt(route, upstreamResponse.getBody());

        return ResponseEntity.status(upstreamResponse.getStatusCode()).body(decryptedBody);
    }

    private URI buildTargetUri(ApiRoute route, String plainBody) {
        if (!RouteUtils.hasTemplateVariables(route.externalPath())) {
            return RouteUtils.buildTargetUri(route);
        }

        Set<String> requiredVariables = RouteUtils.extractTemplateVariables(route.externalPath());
        Map<String, String> variableValues = parseTemplateVariables(plainBody, requiredVariables);
        return RouteUtils.buildTargetUri(route, variableValues);
    }

    private Map<String, String> parseTemplateVariables(String plainBody, Set<String> requiredVariables) {
        try {
            Map<String, Object> payload = objectMapper.readValue(plainBody, new TypeReference<>() {
            });
            Map<String, String> variables = new HashMap<>();

            for (String variable : requiredVariables) {
                Object directValue = payload.get(variable);
                if (directValue != null) {
                    variables.put(variable, String.valueOf(directValue));
                    continue;
                }

                Object pathVariables = payload.get("pathVariables");
                if (pathVariables instanceof Map<?, ?> nested) {
                    Object nestedValue = nested.get(variable);
                    if (nestedValue != null) {
                        variables.put(variable, String.valueOf(nestedValue));
                    }
                }
            }
            return variables;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON body for externalPath template expansion", ex);
        }
    }
}
