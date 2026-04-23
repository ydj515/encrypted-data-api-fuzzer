package com.example.reportserver.parser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 개별 TestN.json 구조 — 타임스탬프 추출용
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatsIndividualReport {

    private Request request;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private String timestamp;
    }
}
