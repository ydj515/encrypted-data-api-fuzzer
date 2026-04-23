package com.example.reportserver.controller.dto;

import lombok.Data;

@Data
public class CatsPublishRequest {

    private String runId;
    private String reportDir;
    private String org;
    private String service;
    private String api;
}
