package com.example.reportserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    private String id;
    private String runId;
    private String api;
    private String name;
    private String endpoint;
    private String httpMethod;
    private int httpStatus;
    private TestStatus status;
    private long durationMs;
    private String failureMsg;
}
