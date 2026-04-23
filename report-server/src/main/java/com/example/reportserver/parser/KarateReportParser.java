package com.example.reportserver.parser;

import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.parser.dto.KarateSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class KarateReportParser {

    private static final DateTimeFormatter RESULT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.KOREAN);

    private final ObjectMapper objectMapper;

    public TestRun parse(
            Path reportDir,
            String runId,
            String org,
            String service,
            String api,
            TestCaseGranularity caseGranularity
    ) {
        Path summaryFile = reportDir.resolve("karate-summary-json.txt");
        try {
            KarateSummary summary = objectMapper.readValue(summaryFile.toFile(), KarateSummary.class);
            int total = summary.getScenariosPassed() + summary.getScenariosFailed();

            return TestRun.builder()
                    .id(runId)
                    .org(org)
                    .service(service)
                    .api(api)
                    .source(TestSource.KARATE)
                    .caseGranularity(caseGranularity)
                    .startedAt(parseResultDate(summary.getResultDate()))
                    .durationMs((long) summary.getElapsedTime())
                    .totalCount(total)
                    .passCount(summary.getScenariosPassed())
                    .failCount(summary.getScenariosFailed())
                    .reportPath("/reports/" + runId + "/report/karate-summary.html")
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LocalDateTime parseResultDate(String resultDate) {
        if (resultDate == null || resultDate.isBlank()) {
            throw new IllegalArgumentException("Karate summary resultDate is required");
        }
        try {
            return LocalDateTime.parse(resultDate, RESULT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid Karate summary resultDate: " + resultDate, e);
        }
    }
}
