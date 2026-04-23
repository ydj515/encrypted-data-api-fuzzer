package com.example.reportserver.cli;

import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.parser.KarateCaseParser;
import com.example.reportserver.parser.KarateReportParser;
import com.example.reportserver.service.RunPublishService;
import com.example.reportserver.service.RunStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class KaratePublishCli {

    public static final String COMMAND = "publish-karate";

    private static final String DEFAULT_REPORT_DIR = "../karate-tests/build/karate-reports/karate-reports";
    private static final String DEFAULT_DATA_DIR = "data/runs";

    private KaratePublishCli() {
    }

    public static void main(String[] args) {
        Map<String, String> options = parseOptions(args);
        Path reportDir = Path.of(value(options, "report-dir", "KARATE_REPORT_DIR", DEFAULT_REPORT_DIR))
                .toAbsolutePath()
                .normalize();
        Path dataDir = Path.of(value(options, "data-dir", "REPORT_DATA_DIR", DEFAULT_DATA_DIR))
                .toAbsolutePath()
                .normalize();
        String org = requiredValue(options, "org", "ORG");
        String service = requiredValue(options, "service", "SERVICE");
        String api = blankToNull(value(options, "api", "API", null));
        String runId = blankToNull(value(options, "run-id", "RUN_ID", null));
        String granularityValue = value(options, "case-granularity", "TEST_CASE_GRANULARITY", null);
        if (granularityValue == null || granularityValue.isBlank()) {
            granularityValue = System.getenv("CASE_GRANULARITY");
        }
        TestCaseGranularity granularity = TestCaseGranularity.from(granularityValue);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunPublishService publishService = new RunPublishService(
                new KarateReportParser(objectMapper),
                new KarateCaseParser(objectMapper),
                new RunStorageService(dataDir, objectMapper)
        );

        String publishedRunId = publishService.publishKarate(runId, reportDir, org, service, api, granularity);

        System.out.println("Karate 리포트 발행 완료: " + publishedRunId);
        System.out.println("저장 위치: " + dataDir.resolve(publishedRunId));
        System.out.println("케이스 단위: " + granularity);
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
