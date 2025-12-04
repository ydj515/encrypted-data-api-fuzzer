package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.config.GatewayProperties;
import com.example.gateway.exception.RouteNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class RouteResolver {

    private final GatewayProperties gatewayProperties;

    public RouteResolver(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public ApiRoute resolve(String org, String service, String api, String method) {
        return gatewayProperties.find(org, service, api, method)
                .orElseThrow(() -> new RouteNotFoundException("Route not found for %s/%s/%s (%s)".formatted(org, service, api, method)));
    }
}
