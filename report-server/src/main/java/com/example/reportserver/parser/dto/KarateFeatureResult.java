package com.example.reportserver.parser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KarateFeatureResult {

    private String name;
    private String resultDate;
    private double durationMillis;
    private int passedCount;
    private List<ScenarioResult> scenarioResults;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScenarioResult {
        private String name;
        private List<String> tags;
        private double durationMillis;
        private boolean failed;
        private long startTime;
        private List<StepResult> stepResults;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepResult {
        private Step step;
        private String stepLog;
        private StepResultDetail result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        private String text;
        private String prefix;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StepResultDetail {
        private double millis;
        private String status;
        private String error;
    }
}
