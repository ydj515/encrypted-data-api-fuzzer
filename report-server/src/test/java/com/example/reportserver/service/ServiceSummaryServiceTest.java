package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.service.dto.ServiceSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceSummaryServiceTest {

    @TempDir
    Path dataDir;

    @Test
    void listServiceSummaries_전체이력의Cases에서Api목록을집계한다() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunStorageService storageService = new RunStorageService(dataDir, objectMapper);
        ServiceSummaryService summaryService = new ServiceSummaryService(storageService);

        storageService.saveRun(run("karate-full-old", null, 10, 1, 0));
        storageService.saveCases("karate-full-old", List.of(testCase("karate-full-old", "createReservation")));
        storageService.saveRun(run("cats-full-new", null, 20, 1, 0));
        storageService.saveCases("cats-full-new", List.of(testCase("cats-full-new", "listResources")));
        storageService.saveRun(run("karate-api-latest", "getResourceDetail", 30, 0, 1));
        storageService.saveCases("karate-api-latest", List.of());

        List<ServiceSummary> summaries = summaryService.listServiceSummaries();

        assertThat(summaries).hasSize(1);
        ServiceSummary summary = summaries.getFirst();
        assertThat(summary.getApiCount()).isEqualTo(3);
        assertThat(summary.getApis()).containsExactly(
                "createReservation",
                "getResourceDetail",
                "listResources"
        );
        assertThat(summary.getLastRunId()).isEqualTo("karate-api-latest");
        assertThat(summary.getLastStatus()).isEqualTo(TestStatus.FAIL);
    }

    private TestRun run(String runId, String api, int dayOfMonth, int passCount, int failCount) {
        return TestRun.builder()
                .id(runId)
                .org("catsOrg")
                .service("booking")
                .api(api)
                .source(TestSource.KARATE)
                .startedAt(LocalDateTime.of(2026, 4, dayOfMonth, 10, 0))
                .durationMs(10)
                .totalCount(passCount + failCount)
                .passCount(passCount)
                .failCount(failCount)
                .reportPath("/reports/" + runId + "/report/karate-summary.html")
                .build();
    }

    private TestCase testCase(String runId, String api) {
        return TestCase.builder()
                .id(runId + "-" + api)
                .runId(runId)
                .api(api)
                .name(api)
                .endpoint("/cats/catsOrg/booking/" + api)
                .httpMethod("POST")
                .httpStatus(200)
                .status(TestStatus.PASS)
                .durationMs(1)
                .build();
    }
}
