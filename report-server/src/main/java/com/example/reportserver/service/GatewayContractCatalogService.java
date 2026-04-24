package com.example.reportserver.service;

import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.contract.GatewayContractCatalogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Slf4j
@Service
public class GatewayContractCatalogService {

    private final Path catalogPath;
    private final Yaml yaml = new Yaml();

    @Autowired
    public GatewayContractCatalogService(
            @Value("${report.gateway-contract-catalog-path}") String catalogPath
    ) {
        this(Path.of(catalogPath));
    }

    public GatewayContractCatalogService(Path catalogPath) {
        this.catalogPath = catalogPath.toAbsolutePath().normalize();
    }

    public List<GatewayContract> listContracts() {
        if (!Files.exists(catalogPath)) {
            log.warn("Gateway contract catalog not found: {}", catalogPath);
            return List.of();
        }

        List<GatewayContractCatalogEntry> entries = readCatalogEntries();
        List<GatewayContract> contracts = new ArrayList<>(entries.size());
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        LinkedHashSet<String> keys = new LinkedHashSet<>();

        for (GatewayContractCatalogEntry entry : entries) {
            if (!ids.add(entry.getId())) {
                throw new IllegalStateException("Duplicate gateway contract id: " + entry.getId());
            }
            if (!keys.add(serviceKey(entry.getOrg(), entry.getService()))) {
                throw new IllegalStateException(
                        "Duplicate gateway contract org/service: " + entry.getOrg() + "/" + entry.getService()
                );
            }

            Path openapiPath = resolveOpenapiPath(entry.getOpenapiPath());
            GatewayContract contract = parseContract(openapiPath, entry.getId());
            validateCatalogEntry(entry, contract);
            contracts.add(contract);
        }

        return List.copyOf(contracts);
    }

    public Optional<GatewayContract> findById(String contractId) {
        if (contractId == null || contractId.isBlank()) {
            return Optional.empty();
        }
        return listContracts().stream()
                .filter(contract -> contractId.equals(contract.getId()))
                .findFirst();
    }

    public Optional<GatewayContract> findByOrgService(String org, String service) {
        if (org == null || org.isBlank() || service == null || service.isBlank()) {
            return Optional.empty();
        }
        return listContracts().stream()
                .filter(contract -> org.equals(contract.getOrg()) && service.equals(contract.getService()))
                .findFirst();
    }

    public Optional<GatewayContract> resolveContract(String contractId, String contractPathValue) {
        if (contractId != null && !contractId.isBlank()) {
            Optional<GatewayContract> byId = findById(contractId);
            if (byId.isPresent()) {
                return byId;
            }
            throw new IllegalArgumentException("Unknown gateway contract id: " + contractId);
        }

        if (contractPathValue == null || contractPathValue.isBlank()) {
            return Optional.empty();
        }

        Path requestedPath = Path.of(contractPathValue).toAbsolutePath().normalize();
        Optional<GatewayContract> catalogMatch = listContracts().stream()
                .filter(contract -> requestedPath.equals(Path.of(contract.getContractPath()).toAbsolutePath().normalize()))
                .findFirst();
        if (catalogMatch.isPresent()) {
            return catalogMatch;
        }

        if (!Files.exists(requestedPath)) {
            throw new IllegalArgumentException("Gateway contract file not found: " + requestedPath);
        }
        return Optional.of(parseContract(requestedPath, null));
    }

    private List<GatewayContractCatalogEntry> readCatalogEntries() {
        Map<String, Object> root = readYamlMap(catalogPath);
        Object rawContracts = root.get("contracts");
        if (!(rawContracts instanceof List<?> contracts)) {
            return List.of();
        }

        List<GatewayContractCatalogEntry> entries = new ArrayList<>();
        for (Object item : contracts) {
            if (!(item instanceof Map<?, ?> contractMap)) {
                continue;
            }
            entries.add(GatewayContractCatalogEntry.builder()
                    .id(requireString(contractMap, "id", catalogPath))
                    .org(requireString(contractMap, "org", catalogPath))
                    .service(requireString(contractMap, "service", catalogPath))
                    .openapiPath(requireString(contractMap, "openapiPath", catalogPath))
                    .build());
        }
        return entries;
    }

    private GatewayContract parseContract(Path openapiPath, String contractId) {
        Map<String, Object> root = readYamlMap(openapiPath);
        String title = stringAt(root, "info", "title");
        String org = extractParameterValue(root, "org");
        String service = extractParameterValue(root, "service");
        Map<String, String> operationIdsByApi = extractOperationIdsByApi(root);

        return GatewayContract.builder()
                .id(contractId)
                .title(title)
                .org(org)
                .service(service)
                .contractPath(openapiPath.toAbsolutePath().normalize().toString())
                .checksum(sha256(openapiPath))
                .apis(List.copyOf(operationIdsByApi.keySet()))
                .operationIdsByApi(Map.copyOf(operationIdsByApi))
                .build();
    }

    private void validateCatalogEntry(GatewayContractCatalogEntry entry, GatewayContract contract) {
        if (!entry.getOrg().equals(contract.getOrg()) || !entry.getService().equals(contract.getService())) {
            throw new IllegalStateException(
                    "Gateway contract catalog mismatch for " + entry.getId()
                            + ": catalog=" + entry.getOrg() + "/" + entry.getService()
                            + ", openapi=" + contract.getOrg() + "/" + contract.getService()
            );
        }
    }

    private Map<String, String> extractOperationIdsByApi(Map<String, Object> root) {
        Object rawPaths = root.get("paths");
        if (!(rawPaths instanceof Map<?, ?> paths)) {
            return Map.of();
        }

        Map<String, String> operationIdsByApi = new TreeMap<>();
        for (Map.Entry<?, ?> entry : paths.entrySet()) {
            String pathKey = asString(entry.getKey());
            if (pathKey == null || !pathKey.startsWith("/cats/{org}/{service}/")) {
                continue;
            }

            String api = pathKey.substring("/cats/{org}/{service}/".length());
            if (api.isBlank() || !(entry.getValue() instanceof Map<?, ?> pathItem)) {
                continue;
            }

            String operationId = extractOperationId(pathItem);
            if (operationId != null && !operationId.isBlank()) {
                operationIdsByApi.put(api, operationId);
            }
        }
        return operationIdsByApi;
    }

    private String extractOperationId(Map<?, ?> pathItem) {
        for (String method : List.of("post", "get", "put", "patch", "delete")) {
            Object rawOperation = pathItem.get(method);
            if (rawOperation instanceof Map<?, ?> operationMap) {
                String operationId = asString(operationMap.get("operationId"));
                if (operationId != null && !operationId.isBlank()) {
                    return operationId;
                }
            }
        }
        return null;
    }

    private String extractParameterValue(Map<String, Object> root, String parameterName) {
        Object rawComponents = root.get("components");
        if (!(rawComponents instanceof Map<?, ?> components)) {
            throw new IllegalArgumentException("Missing components in gateway contract: " + root);
        }
        Object rawParameters = components.get("parameters");
        if (!(rawParameters instanceof Map<?, ?> parameters)) {
            throw new IllegalArgumentException("Missing components.parameters in gateway contract");
        }

        for (Map.Entry<?, ?> entry : parameters.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> parameterMap)) {
                continue;
            }
            String key = asString(entry.getKey());
            String name = asString(parameterMap.get("name"));
            if (!parameterName.equalsIgnoreCase(key) && !parameterName.equalsIgnoreCase(name)) {
                continue;
            }

            String example = asString(parameterMap.get("example"));
            if (example != null && !example.isBlank()) {
                return example;
            }

            Object rawSchema = parameterMap.get("schema");
            if (rawSchema instanceof Map<?, ?> schemaMap) {
                Object rawEnum = schemaMap.get("enum");
                if (rawEnum instanceof List<?> enumValues && !enumValues.isEmpty()) {
                    String enumValue = asString(enumValues.getFirst());
                    if (enumValue != null && !enumValue.isBlank()) {
                        return enumValue;
                    }
                }
                String defaultValue = asString(schemaMap.get("default"));
                if (defaultValue != null && !defaultValue.isBlank()) {
                    return defaultValue;
                }
            }
        }

        throw new IllegalArgumentException("Cannot resolve parameter value from gateway contract: " + parameterName);
    }

    private String stringAt(Map<String, Object> root, String firstKey, String secondKey) {
        Object first = root.get(firstKey);
        if (!(first instanceof Map<?, ?> map)) {
            return null;
        }
        return asString(map.get(secondKey));
    }

    private Path resolveOpenapiPath(String openapiPath) {
        Path path = Path.of(openapiPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return catalogPath.getParent().resolve(path).toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYamlMap(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("YAML root must be an object: " + path);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = asString(entry.getKey());
                result.put(key, (Object) entry.getValue());
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String requireString(Map<?, ?> map, String key, Path sourcePath) {
        String value = asString(map.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required key '" + key + "' in " + sourcePath);
        }
        return value;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String serviceKey(String org, String service) {
        return org + "/" + service;
    }
}
