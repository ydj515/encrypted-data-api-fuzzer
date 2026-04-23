package com.example.reportserver.service.dto;

import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RunFilter {
    String org;
    String service;
    String api;
    TestSource source;
    TestStatus status;
    Integer httpStatus;
    String caseName;
    String endpoint;
    LocalDateTime from;
    LocalDateTime to;
}
