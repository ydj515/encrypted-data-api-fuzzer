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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceSummaryServiceTest {

    @TempDir
    Path dataDir;

    @TempDir
    Path contractDir;

    @Test
    void listServiceSummaries_계약카탈로그기반으로미실행서비스도노출한다() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunStorageService storageService = new RunStorageService(dataDir, objectMapper);
        GatewayContractCatalogService contractCatalogService =
                new GatewayContractCatalogService(writeCatalog(contractDir));
        ServiceSummaryService summaryService = new ServiceSummaryService(storageService, contractCatalogService);

        storageService.saveRun(run("karate-full-old", null, 10, 1, 0));
        storageService.saveCases("karate-full-old", List.of(testCase("karate-full-old", "createReservation")));
        storageService.saveRun(run("cats-full-new", null, 20, 1, 0));
        storageService.saveCases("cats-full-new", List.of(testCase("cats-full-new", "listResources")));
        storageService.saveRun(run("karate-api-latest", "getResourceDetail", 30, 0, 1));
        storageService.saveCases("karate-api-latest", List.of());

        List<ServiceSummary> summaries = summaryService.listServiceSummaries();

        assertThat(summaries).hasSize(2);

        ServiceSummary bookingSummary = summaries.getFirst();
        assertThat(bookingSummary.getContractId()).isEqualTo("gw-cats-booking");
        assertThat(bookingSummary.getApiCount()).isEqualTo(3);
        assertThat(bookingSummary.getApis()).containsExactly(
                "createReservation",
                "getResourceDetail",
                "listResources"
        );
        assertThat(bookingSummary.getLastRunId()).isEqualTo("karate-api-latest");
        assertThat(bookingSummary.getLastStatus()).isEqualTo(TestStatus.FAIL);

        ServiceSummary orgASummary = summaries.get(1);
        assertThat(orgASummary.getContractId()).isEqualTo("gw-orga-reservation");
        assertThat(orgASummary.getOrg()).isEqualTo("orgA");
        assertThat(orgASummary.getService()).isEqualTo("reservation");
        assertThat(orgASummary.getApiCount()).isEqualTo(2);
        assertThat(orgASummary.getApis()).containsExactly("getResourceDetail", "listResources");
        assertThat(orgASummary.getLastRunId()).isNull();
        assertThat(orgASummary.getLastStatus()).isNull();
    }

    @Test
    void listServiceSummaries_계약에없는케이스Api는서비스목록에포함하지않는다() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RunStorageService storageService = new RunStorageService(dataDir, objectMapper);
        GatewayContractCatalogService contractCatalogService =
                new GatewayContractCatalogService(writeCatalog(contractDir));
        ServiceSummaryService summaryService = new ServiceSummaryService(storageService, contractCatalogService);

        storageService.saveRun(TestRun.builder()
                .id("support-run")
                .org("orgA")
                .service("reservation")
                .source(TestSource.KARATE)
                .startedAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .durationMs(10)
                .totalCount(1)
                .passCount(1)
                .failCount(0)
                .reportPath("/reports/support-run/report/karate-summary.html")
                .build());
        storageService.saveCases("support-run", List.of(
                testCase("support-run", "listResources"),
                testCase("support-run", "createTicket")
        ));

        List<ServiceSummary> summaries = summaryService.listServiceSummaries();

        ServiceSummary orgASummary = summaries.stream()
                .filter(summary -> "orgA".equals(summary.getOrg()) && "reservation".equals(summary.getService()))
                .findFirst()
                .orElseThrow();

        assertThat(orgASummary.getApis()).containsExactly("getResourceDetail", "listResources");
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

    private Path writeCatalog(Path baseDir) throws Exception {
        Files.createDirectories(baseDir);
        Files.writeString(baseDir.resolve("catalog.yaml"), """
                version: 1
                contracts:
                  - id: gw-cats-booking
                    org: catsOrg
                    service: booking
                    openapiPath: cats-gw-openapi.yaml
                  - id: gw-orga-reservation
                    org: orgA
                    service: reservation
                    openapiPath: orgA-reservation-gw-openapi.yaml
                """);
        writeContract(
                baseDir.resolve("cats-gw-openapi.yaml"),
                "catsOrg",
                "booking",
                List.of("createReservation", "getResourceDetail", "listResources")
        );
        writeContract(
                baseDir.resolve("orgA-reservation-gw-openapi.yaml"),
                "orgA",
                "reservation",
                List.of("listResources", "getResourceDetail")
        );
        return baseDir.resolve("catalog.yaml");
    }

    private void writeContract(Path path, String org, String service, List<String> apis) throws Exception {
        StringBuilder paths = new StringBuilder();
        for (String api : apis) {
            paths.append("""
                      /cats/{org}/{service}/%s:
                        post:
                          operationId: %sViaGateway
                    """.formatted(api, api));
        }

        Files.writeString(path, """
                openapi: 3.0.3
                info:
                  title: %s %s Gateway API Contract
                paths:
                %s
                components:
                  parameters:
                    Org:
                      name: org
                      in: path
                      required: true
                      schema:
                        type: string
                        enum: [%s]
                      example: %s
                    Service:
                      name: service
                      in: path
                      required: true
                      schema:
                        type: string
                        enum: [%s]
                      example: %s
                """.formatted(org, service, paths, org, org, service, service));
    }
}
