package com.example.reportserver.model;

import java.util.Locale;

public enum TestCaseGranularity {
    SCENARIO,
    HTTP_CALL,
    BOTH;

    public static TestCaseGranularity from(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "scenario", "scenarios" -> SCENARIO;
            case "http", "http-call", "http_call", "httpcall", "call", "calls" -> HTTP_CALL;
            case "both", "all" -> BOTH;
            default -> throw new IllegalArgumentException(
                    "Unsupported TEST_CASE_GRANULARITY: " + value + " (expected scenario, http-call, or both)"
            );
        };
    }

    public boolean includesScenario() {
        return this == SCENARIO || this == BOTH;
    }

    public boolean includesHttpCall() {
        return this == HTTP_CALL || this == BOTH;
    }
}
