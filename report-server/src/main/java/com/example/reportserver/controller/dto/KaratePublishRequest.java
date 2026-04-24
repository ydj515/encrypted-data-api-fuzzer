package com.example.reportserver.controller.dto;

import lombok.Data;

@Data
public class KaratePublishRequest {

    private String runId;
    private String reportDir;
    private String contractId;
    private String contractPath;
    private String org;
    private String service;
    private String api;
    private String caseGranularity;
}
