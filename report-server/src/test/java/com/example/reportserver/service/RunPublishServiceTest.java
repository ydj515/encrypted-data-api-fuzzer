package com.example.reportserver.service;

import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.parser.KarateCaseParser;
import com.example.reportserver.parser.KarateReportParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunPublishServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void publishKarateWritesCompleteRunDirectory() throws Exception {
        Path sourceReportDir = tempDir.resolve("karate-report");
        Path dataDir = tempDir.resolve("runs");
        writeReport(sourceReportDir);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunStorageService storageService = new RunStorageService(dataDir, objectMapper);
        RunPublishService publishService = new RunPublishService(
                new KarateReportParser(objectMapper),
                new KarateCaseParser(objectMapper),
                storageService
        );

        String runId = publishService.publishKarate(
                "karate-test-run",
                sourceReportDir,
                "catsOrg",
                "booking",
                null,
                TestCaseGranularity.BOTH
        );

        Path runDir = dataDir.resolve(runId);
        assertThat(Files.exists(runDir.resolve("meta.json"))).isTrue();
        assertThat(Files.exists(runDir.resolve("cases.json"))).isTrue();
        assertThat(Files.exists(runDir.resolve("report/karate-summary.html"))).isTrue();
        try (Stream<Path> files = Files.list(dataDir)) {
            assertThat(files.filter(path -> path.getFileName().toString().contains(".tmp")).toList()).isEmpty();
        }

        TestRun run = storageService.findRun(runId).orElseThrow();
        assertThat(run.getReportPath()).isEqualTo("/reports/karate-test-run/report/karate-summary.html");
        assertThat(run.getCaseGranularity()).isEqualTo(TestCaseGranularity.BOTH);
        assertThat(storageService.findCases(runId)).hasSize(2);
    }

    @Test
    void rejectRunIdWithPathSegments() throws Exception {
        Path sourceReportDir = tempDir.resolve("karate-report");
        writeReport(sourceReportDir);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunPublishService publishService = new RunPublishService(
                new KarateReportParser(objectMapper),
                new KarateCaseParser(objectMapper),
                new RunStorageService(tempDir.resolve("runs"), objectMapper)
        );

        assertThatThrownBy(() -> publishService.publishKarate(
                "a/b",
                sourceReportDir,
                "catsOrg",
                "booking",
                null,
                TestCaseGranularity.BOTH
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Run ID may contain only");
    }

    private void writeReport(Path reportDir) throws Exception {
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("karate-summary.html"), "<html></html>");
        Files.writeString(reportDir.resolve("karate-summary-json.txt"), """
                {
                  "resultDate": "2026-04-23 01:14:15 오전",
                  "elapsedTime": 42.0,
                  "scenariosPassed": 1,
                  "scenariosfailed": 0
                }
                """);
        Files.writeString(reportDir.resolve("scenarios.booking.create-reservation.karate-json.txt"), """
                {
                  "scenarioResults": [
                    {
                      "name": "reservation lifecycle",
                      "durationMillis": 25.0,
                      "failed": false,
                      "tags": ["service=booking", "api=createReservation"],
                      "stepResults": [
                        {
                          "result": {"millis": 10.0, "status": "passed"},
                          "stepLog": "1 > POST http://localhost:28080/cats/catsOrg/booking/createReservation\\n1 < 201\\n"
                        }
                      ]
                    }
                  ]
                }
                """);
    }
}
