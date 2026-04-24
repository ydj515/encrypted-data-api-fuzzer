package com.example.reportserver.service.dto;

import com.example.reportserver.model.TestStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ServiceSummary {
    String contractId;
    String org;
    String service;
    List<String> apis;
    int apiCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastRunAt;
    TestStatus lastStatus;
    String lastRunId;
}
