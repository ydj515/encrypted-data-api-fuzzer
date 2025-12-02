package com.example.gateway.service;

import com.jayway.jsonpath.JsonPath;
import com.example.gateway.config.ApiRoute;
import com.example.gateway.config.GatewayProperties;
import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
import com.example.gateway.exception.RouteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GatewayProxyService {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyService.class);

    private final CryptoModule cryptoModule;
    private final ChecksumModule checksumModule;
    private final GatewayProperties gatewayProperties;
    private final RestClient restClient;

    public GatewayProxyService(CryptoModule cryptoModule,
                               ChecksumModule checksumModule,
                               GatewayProperties gatewayProperties,
                               RestClient.Builder restClientBuilder) {
        this.cryptoModule = cryptoModule;
        this.checksumModule = checksumModule;
        this.gatewayProperties = gatewayProperties;
        this.restClient = restClientBuilder.build();
    }

    public ResponseEntity<String> proxyPost(String org, String service, String api, String plainBody) {
        ApiRoute route = resolveRoute(org, service, api, "POST");

        String encryptedData = cryptoModule.encrypt(route.key(), plainBody);
        String checksum = checksumModule.checksum(encryptedData);

        URI targetUri = buildTargetUri(route);
        HttpHeaders headers = buildHeaders(route, checksum);

        Map<String, Object> requestBody = Map.of("data", encryptedData);

        ResponseEntity<String> upstreamResponse = executeExternalPost(targetUri, headers, requestBody);
        String decryptedBody = decryptResponseBody(route, upstreamResponse.getBody());

        return ResponseEntity.status(upstreamResponse.getStatusCode()).body(decryptedBody);
    }

    private ApiRoute resolveRoute(String org, String service, String api, String method) {
        return gatewayProperties.find(org, service, api, method)
                .orElseThrow(() -> new RouteNotFoundException("Route not found for %s/%s/%s (%s)".formatted(org, service, api, method)));
    }

    private URI buildTargetUri(ApiRoute route) {
        if (route.host() == null || route.host().isBlank()) {
            throw new IllegalStateException("Host not configured for route " + route);
        }
        if (route.externalPath() == null || route.externalPath().isBlank()) {
            throw new IllegalStateException("External path not configured for route " + route);
        }

        String externalPath = route.externalPath().startsWith("/")
                ? route.externalPath()
                : "/" + route.externalPath();

        return UriComponentsBuilder.fromHttpUrl(route.host())
                .path(externalPath)
                .build()
                .toUri();
    }

    private HttpHeaders buildHeaders(ApiRoute route, String checksum) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Checksum", checksum);

        Optional.ofNullable(route.apiKey()).ifPresent(value -> headers.set("X-Api-Key", value));
        Optional.ofNullable(route.insCode()).ifPresent(value -> headers.set("X-Ins-Code", value));
        return headers;
    }

    private ResponseEntity<String> executeExternalPost(URI uri, HttpHeaders headers, Map<String, Object> requestBody) {
        try {
            return restClient.post()
                    .uri(uri)
                    .headers(httpHeaders -> httpHeaders.putAll(headers))
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            HttpHeaders responseHeaders = ex.getResponseHeaders() != null ? ex.getResponseHeaders() : HttpHeaders.EMPTY;
            HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.status(status)
                    .headers(responseHeaders)
                    .body(ex.getResponseBodyAsString());
        }
    }

    private String decryptResponseBody(ApiRoute route, String responseBody) {
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
