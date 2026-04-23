package com.example.reportserver.service;

import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RunStorageServiceTest {

    @TempDir
    Path dataDir;

    private RunStorageService storageService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        storageService = new RunStorageService(dataDir, objectMapper);
    }

    @Test
    void findRun_legacyKarateReportPath를SummaryHtml로보정한다() {
        storageService.saveRun(run("karate-legacy", TestSource.KARATE, "/reports/karate-legacy"));

        TestRun run = storageService.findRun("karate-legacy").orElseThrow();

        assertThat(run.getReportPath()).isEqualTo("/reports/karate-legacy/report/karate-summary.html");
    }

    @Test
    void listAllRuns_legacyCatsReportPath를IndexHtml로보정한다() {
        storageService.saveRun(run("cats-legacy", TestSource.CATS, "/reports/cats-legacy"));

        assertThat(storageService.listAllRuns())
                .singleElement()
                .extracting(TestRun::getReportPath)
                .isEqualTo("/reports/cats-legacy/report/index.html");
    }

    @Test
    void findRun_정상ReportPath는유지한다() {
        storageService.saveRun(run(
                "karate-current",
                TestSource.KARATE,
                "/reports/karate-current/report/karate-summary.html"
        ));

        TestRun run = storageService.findRun("karate-current").orElseThrow();

        assertThat(run.getReportPath()).isEqualTo("/reports/karate-current/report/karate-summary.html");
    }

    private TestRun run(String runId, TestSource source, String reportPath) {
        return TestRun.builder()
                .id(runId)
                .org("catsOrg")
                .service("booking")
                .source(source)
                .startedAt(LocalDateTime.of(2026, 4, 23, 10, 0))
                .durationMs(1)
                .totalCount(1)
                .passCount(1)
                .failCount(0)
                .reportPath(reportPath)
                .build();
    }
}
