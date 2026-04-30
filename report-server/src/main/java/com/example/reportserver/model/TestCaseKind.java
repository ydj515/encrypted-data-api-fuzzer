package com.example.reportserver.model;

public enum TestCaseKind {
    SINGLE_API("single-api", "단건"),
    SCENARIO("scenario", "시나리오");

    private final String tagValue;
    private final String label;

    TestCaseKind(String tagValue, String label) {
        this.tagValue = tagValue;
        this.label = label;
    }

    public String getTagValue() {
        return tagValue;
    }

    public String getLabel() {
        return label;
    }

    public static TestCaseKind fromTagValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (TestCaseKind kind : values()) {
            if (kind.tagValue.equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return null;
    }
}
