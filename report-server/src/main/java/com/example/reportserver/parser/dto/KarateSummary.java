package com.example.reportserver.parser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KarateSummary {

    private String resultDate;
    private double elapsedTime;
    private int scenariosPassed;

    @JsonProperty("scenariosfailed")
    private int scenariosFailed;

    private List<FeatureItem> featureSummary;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeatureItem {
        private String packageQualifiedName;
        private String relativePath;
        private int scenarioCount;
        private int passedCount;
        private int failedCount;
        private double durationMillis;
        private boolean failed;
    }
}
