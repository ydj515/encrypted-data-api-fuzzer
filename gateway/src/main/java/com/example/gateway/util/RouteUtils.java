package com.example.gateway.util;

import com.example.gateway.config.ApiRoute;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RouteUtils {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^}/?]+)}");

    private RouteUtils() {
        // Utility class
    }

    public static URI buildTargetUri(ApiRoute route) {
        return buildTargetUri(route, Map.of());
    }

    public static URI buildTargetUri(ApiRoute route, Map<String, String> templateVariables) {
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

        Set<String> expectedVariables = extractTemplateVariables(externalPath);
        for (String variableName : expectedVariables) {
            String variableValue = templateVariables.get(variableName);
            if (variableValue == null || variableValue.isBlank()) {
                throw new IllegalArgumentException("Missing template variable: " + variableName);
            }
        }

        String uriTemplate = host + externalPath;
        return UriComponentsBuilder.fromUriString(uriTemplate)
                .buildAndExpand(templateVariables)
                .encode()
                .toUri();
    }

    public static Set<String> extractTemplateVariables(String externalPath) {
        if (externalPath == null || externalPath.isBlank()) {
            return Set.of();
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(externalPath);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    public static boolean hasTemplateVariables(String externalPath) {
        return !extractTemplateVariables(externalPath).isEmpty();
    }
}
