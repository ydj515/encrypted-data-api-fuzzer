package com.example.reportserver.cli;

import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.parser.CatsReportParser;
import com.example.reportserver.service.GatewayContractCatalogService;
import com.example.reportserver.service.RunPublishService;
import com.example.reportserver.service.RunStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CatsPublishCli {

    public static final String COMMAND = "publish-cats";

    private static final String DEFAULT_REPORT_DIR = "../cats-report";
    private static final String DEFAULT_DATA_DIR = "data/runs";
    private static final String DEFAULT_GATEWAY_CONTRACT_CATALOG_PATH = "../docs/openapi/gateway/catalog.yaml";

    private CatsPublishCli() {
    }

    public static void main(String[] args) {
        Map<String, String> options = parseOptions(args);
        Path reportDir = Path.of(value(options, "report-dir", "CATS_REPORT_DIR", DEFAULT_REPORT_DIR))
                .toAbsolutePath()
                .normalize();
        Path dataDir = Path.of(value(options, "data-dir", "REPORT_DATA_DIR", DEFAULT_DATA_DIR))
                .toAbsolutePath()
                .normalize();
        String api = blankToNull(value(options, "api", "API", null));
        String runId = blankToNull(value(options, "run-id", "RUN_ID", null));
        String contractId = blankToNull(value(options, "contract-id", "CONTRACT_ID", null));
        String contractPath = blankToNull(value(options, "contract-path", "CONTRACT_PATH", null));
        Path catalogPath = Path.of(value(
                        options,
                        "contract-catalog-path",
                        "GATEWAY_CONTRACT_CATALOG_PATH",
                        DEFAULT_GATEWAY_CONTRACT_CATALOG_PATH
                ))
                .toAbsolutePath()
                .normalize();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunPublishService publishService = RunPublishService.forCats(
                new CatsReportParser(objectMapper),
                new RunStorageService(dataDir, objectMapper)
        );
        GatewayContractCatalogService contractCatalogService = new GatewayContractCatalogService(catalogPath);
        GatewayContract contract = contractCatalogService.resolveContract(contractId, contractPath)
                .orElseThrow(() -> new IllegalArgumentException(
                        "--contract-id/CONTRACT_ID or --contract-path/CONTRACT_PATH is required"
                ));

        String publishedRunId = publishService.publishCats(runId, reportDir, contract, api);

        System.out.println("CATS 리포트 발행 완료: " + publishedRunId);
        System.out.println("저장 위치: " + dataDir.resolve(publishedRunId));
        System.out.println("계약 ID: " + (contract.getId() != null ? contract.getId() : "(unregistered)"));
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unsupported argument: " + arg);
            }

            String withoutPrefix = arg.substring(2);
            int equalsIndex = withoutPrefix.indexOf('=');
            if (equalsIndex >= 0) {
                options.put(withoutPrefix.substring(0, equalsIndex), withoutPrefix.substring(equalsIndex + 1));
                continue;
            }

            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for argument: " + arg);
            }
            options.put(withoutPrefix, args[++i]);
        }
        return options;
    }

    private static String value(Map<String, String> options, String optionName, String envName, String defaultValue) {
        String optionValue = options.get(optionName);
        if (optionValue != null) {
            return optionValue;
        }
        String envValue = System.getenv(envName);
        return envValue == null ? defaultValue : envValue;
    }

    private static String requiredValue(Map<String, String> options, String optionName, String envName) {
        String value = value(options, optionName, envName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("--" + optionName + " or " + envName + " is required");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
