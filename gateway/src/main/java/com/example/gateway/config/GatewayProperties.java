package com.example.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<ApiRoute> apis = new ArrayList<>();

    public Optional<ApiRoute> find(String org, String service, String api, String method) {
        return apis.stream()
                .filter(route -> route.org().equals(org)
                        && route.service().equals(service)
                        && route.api().equals(api)
                        && route.method().equalsIgnoreCase(method))
                .findFirst();
    }
}
