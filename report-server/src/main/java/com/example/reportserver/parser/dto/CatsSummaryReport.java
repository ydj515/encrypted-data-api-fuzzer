package com.example.reportserver.parser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatsSummaryReport {

    private List<TestCaseItem> testCases;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestCaseItem {
        private String id;
        private String scenario;
        private String result;
        private String resultReason;
        private String resultDetails;
        private String path;
        private String fuzzer;
        private double timeToExecuteInSec;
        private String timeToExecuteInMs;
        private String httpMethod;
        private boolean switchedResult;
        private int httpResponseCode;
    }
}
