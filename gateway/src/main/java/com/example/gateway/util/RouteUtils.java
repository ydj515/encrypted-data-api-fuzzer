package com.example.gateway.util;

import com.example.gateway.config.ApiRoute;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public final class RouteUtils {

    private RouteUtils() {
        // Utility class
    }

    public static URI buildTargetUri(ApiRoute route) {
        String host = route.host();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Host not configured for route " + route);
        }

        String routeExternalPath = route.externalPath();
        if (routeExternalPath == null || routeExternalPath.isBlank()) {
            throw new IllegalStateException("External path not configured for route " + route);
        }

        String externalPath = routeExternalPath.startsWith("/")
                ? routeExternalPath
                : "/" + routeExternalPath;

        return UriComponentsBuilder.fromUriString(host)
                .path(externalPath)
                .build()
                .toUri();
    }
}
