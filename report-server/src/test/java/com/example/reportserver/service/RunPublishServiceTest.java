package com.example.reportserver.service;

import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.parser.CatsReportParser;
import com.example.reportserver.parser.KarateCaseParser;
import com.example.reportserver.parser.KarateReportParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
        RunPublishService publishService = RunPublishService.forKarate(
                new KarateReportParser(objectMapper),
                new KarateCaseParser(objectMapper),
                storageService
        );

        String runId = publishService.publishKarate(
                "karate-test-run",
                sourceReportDir,
                contract("gw-cats-booking", "catsOrg", "booking"),
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
        assertThat(run.getContractId()).isEqualTo("gw-cats-booking");
        assertThat(run.getContractPath()).isEqualTo("/contracts/catsOrg-booking-gw-openapi.yaml");
        assertThat(run.getContractChecksum()).isEqualTo("checksum-catsOrg-booking");
        assertThat(storageService.findCases(runId)).hasSize(2);
    }

    @Test
    void publishCatsWritesCompleteRunDirectory() throws Exception {
        Path sourceReportDir = tempDir.resolve("cats-report");
        Path dataDir = tempDir.resolve("runs");
        writeCatsReport(sourceReportDir);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunStorageService storageService = new RunStorageService(dataDir, objectMapper);
        RunPublishService publishService = RunPublishService.forCats(
                new CatsReportParser(objectMapper),
                storageService
        );

        String runId = publishService.publishCats(
                "cats-test-run",
                sourceReportDir,
                contract("gw-cats-booking", "catsOrg", "booking"),
                "listResources"
        );

        Path runDir = dataDir.resolve(runId);
        assertThat(Files.exists(runDir.resolve("meta.json"))).isTrue();
        assertThat(Files.exists(runDir.resolve("cases.json"))).isTrue();
        assertThat(Files.exists(runDir.resolve("report/index.html"))).isTrue();

        TestRun run = storageService.findRun(runId).orElseThrow();
        assertThat(run.getSource()).isEqualTo(TestSource.CATS);
        assertThat(run.getTotalCount()).isEqualTo(1);
        assertThat(run.getPassCount()).isEqualTo(1);
        assertThat(run.getOperationId()).isEqualTo("listResourcesViaGateway");
        assertThat(run.getReportPath()).isEqualTo("/reports/cats-test-run/report/index.html");
        assertThat(storageService.findCases(runId))
                .hasSize(1)
                .first()
                .extracting("api", "endpoint", "httpMethod")
                .containsExactly("listResources", "/cats/catsOrg/booking/listResources", "POST");
    }

    @Test
    void rejectRunIdWithPathSegments() throws Exception {
        Path sourceReportDir = tempDir.resolve("karate-report");
        writeReport(sourceReportDir);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunPublishService publishService = RunPublishService.forKarate(
                new KarateReportParser(objectMapper),
                new KarateCaseParser(objectMapper),
                new RunStorageService(tempDir.resolve("runs"), objectMapper)
        );

        assertThatThrownBy(() -> publishService.publishKarate(
                "a/b",
                sourceReportDir,
                contract("gw-cats-booking", "catsOrg", "booking"),
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

    private void writeCatsReport(Path reportDir) throws Exception {
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("index.html"), "<html></html>");
        Files.writeString(reportDir.resolve("cats-summary-report.json"), """
                {
                  "testCases": [
                    {
                      "id": "Test 1",
                      "scenario": "happy flow",
                      "result": "success",
                      "path": "/cats/{org}/{service}/listResources",
                      "fuzzer": "HappyPath",
                      "httpMethod": "post",
                      "httpResponseCode": 200,
                      "timeToExecuteInMs": "12"
                    }
                  ]
                }
                """);
        Files.writeString(reportDir.resolve("Test1.json"), """
                {
                  "request": {
                    "timestamp": "Mon, 2 Mar 2026 16:27:16 +0900"
                  }
                }
                """);
    }

    private GatewayContract contract(String id, String org, String service) {
        return GatewayContract.builder()
                .id(id)
                .org(org)
                .service(service)
                .contractPath("/contracts/%s-%s-gw-openapi.yaml".formatted(org, service))
                .checksum("checksum-" + org + "-" + service)
                .apis(List.of("createReservation", "listResources"))
                .operationIdsByApi(Map.of(
                        "createReservation", "createReservationViaGateway",
                        "listResources", "listResourcesViaGateway"
                ))
                .build();
    }
}
