package com.example.reportserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    private String id;
    private String org;
    private String service;
    private String api;
    private String operationId;
    private String contractId;
    private String contractPath;
    private String contractChecksum;
    private TestSource source;
    private TestCaseGranularity caseGranularity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    private long durationMs;
    private int totalCount;
    private int passCount;
    private int failCount;
    private String reportPath;
}
