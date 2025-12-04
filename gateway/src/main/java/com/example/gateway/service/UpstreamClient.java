package com.example.gateway.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;

@Component
public class UpstreamClient {

    private final RestClient restClient;

    public UpstreamClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ResponseEntity<String> post(URI uri, HttpHeaders headers, Map<String, Object> requestBody) {
        try {
            return restClient.post()
                    .uri(uri)
                    .headers(httpHeaders -> httpHeaders.putAll(headers))
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException ex) {
            HttpHeaders responseHeaders = ex.getResponseHeaders() != null ? ex.getResponseHeaders() : HttpHeaders.EMPTY;
            HttpStatusCode status;
            try {
                status = ex.getStatusCode();
            } catch (Exception ignored) {
                status = HttpStatusCode.valueOf(500);
            }
            return ResponseEntity.status(status)
                    .headers(responseHeaders)
                    .body(ex.getResponseBodyAsString());
        }
    }
}
