package com.example.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<ApiRoute> apis = new ArrayList<>();
    private Map<String, ApiRoute> routeIndex = Map.of();

    public void setApis(List<ApiRoute> apis) {
        this.apis = apis != null ? new ArrayList<>(apis) : new ArrayList<>();
        rebuildRouteIndex();
    }

    public Optional<ApiRoute> find(String org, String service, String api, String method) {
        String key = cacheKey(org, service, api, method);
        return Optional.ofNullable(routeIndex.get(key));
    }

    private void rebuildRouteIndex() {
        Map<String, ApiRoute> index = new HashMap<>();
        for (ApiRoute route : apis) {
            index.put(cacheKey(route.org(), route.service(), route.api(), route.method()), route);
        }
        this.routeIndex = Map.copyOf(index);
    }

    private String cacheKey(String org, String service, String api, String method) {
        return "%s|%s|%s|%s".formatted(org, service, api, method.toUpperCase());
    }
}
