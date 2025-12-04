package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GatewayHeaderFactory {

    public HttpHeaders build(ApiRoute route, String checksum) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(GatewayHeaderNames.X_CHECKSUM, checksum);

        Optional.ofNullable(route.apiKey()).ifPresent(value -> headers.set(GatewayHeaderNames.X_API_KEY, value));
        Optional.ofNullable(route.insCode()).ifPresent(value -> headers.set(GatewayHeaderNames.X_INS_CODE, value));
        return headers;
    }
}
