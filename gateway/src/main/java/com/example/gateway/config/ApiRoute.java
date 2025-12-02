package com.example.gateway.config;

public record ApiRoute(
        String org,
        String service,
        String api,
        String method,
        String host,
        String externalPath,
        String key,
        String apiKey,
        String insCode
) {
}
