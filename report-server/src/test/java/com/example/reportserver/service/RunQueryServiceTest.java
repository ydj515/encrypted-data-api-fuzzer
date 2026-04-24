package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.service.dto.RunFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RunQueryServiceTest {

    @TempDir
    Path dataDir;

    @TempDir
    Path contractDir;

    private RunStorageService storageService;
    private RunQueryService queryService;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        storageService = new RunStorageService(dataDir, objectMapper);
        GatewayContractCatalogService contractCatalogService =
                new GatewayContractCatalogService(writeCatalog(contractDir));
        queryService = new RunQueryService(storageService, contractCatalogService);
    }

    @Test
    void findRuns_api필터는전체실행의케이스Api까지포함한다() {
        storageService.saveRun(run("full-run", null, 1, TestSource.CATS, 1, 1));
        storageService.saveCases("full-run", List.of(
                testCase("full-run", "createReservation", 404, TestStatus.FAIL),
                testCase("full-run", "listResources", 200, TestStatus.PASS)
        ));

        List<TestRun> runs = queryService.findRuns(RunFilter.builder()
                .org("catsOrg")
                .service("booking")
                .api("createReservation")
                .build());

        assertThat(runs).extracting(TestRun::getId).containsExactly("full-run");
    }

    @Test
    void findRuns_api와HttpStatus필터는같은케이스에서동시에만족해야한다() {
        storageService.saveRun(run("full-run", null, 1, TestSource.CATS, 1, 1));
        storageService.saveCases("full-run", List.of(
                testCase("full-run", "createReservation", 404, TestStatus.FAIL),
                testCase("full-run", "listResources", 200, TestStatus.PASS)
        ));

        List<TestRun> mismatched = queryService.findRuns(RunFilter.builder()
                .org("catsOrg")
                .service("booking")
                .api("createReservation")
                .httpStatus(200)
                .build());
        List<TestRun> matched = queryService.findRuns(RunFilter.builder()
                .org("catsOrg")
                .service("booking")
                .api("createReservation")
                .httpStatus(404)
                .build());

        assertThat(mismatched).isEmpty();
        assertThat(matched).extracting(TestRun::getId).containsExactly("full-run");
    }

    @Test
    void findAvailableApis_메타와케이스에서등장한Api를모두반환한다() {
        storageService.saveRun(run("full-run", null, 1, TestSource.CATS, 1, 0));
        storageService.saveCases("full-run", List.of(
                testCase("full-run", "createReservation", 200, TestStatus.PASS),
                testCase("full-run", "listResources", 200, TestStatus.PASS)
        ));
        storageService.saveRun(run("single-api-run", "getResourceDetail", 2, TestSource.KARATE, 1, 0));
        storageService.saveCases("single-api-run", List.of());

        assertThat(queryService.findAvailableApis("catsOrg", "booking"))
                .containsExactly("createReservation", "getResourceDetail", "listResources");
    }

    @Test
    void findAvailableApis_실행이력이없어도계약카탈로그의Api를반환한다() {
        assertThat(queryService.findAvailableApis("orgB", "support"))
                .containsExactly("createTicket", "listDevices");
    }

    @Test
    void findAvailableApis_계약에없는케이스Api는제외한다() {
        storageService.saveRun(TestRun.builder()
                .id("support-run")
                .org("orgB")
                .service("support")
                .source(TestSource.KARATE)
                .startedAt(LocalDateTime.of(2026, 4, 3, 10, 0))
                .durationMs(10)
                .totalCount(1)
                .passCount(1)
                .failCount(0)
                .reportPath("/reports/support-run/report/index.html")
                .build());
        storageService.saveCases("support-run", List.of(
                TestCase.builder()
                        .id("support-run-listDevices")
                        .runId("support-run")
                        .api("listDevices")
                        .name("listDevices case")
                        .endpoint("/cats/orgB/support/listDevices")
                        .httpMethod("POST")
                        .httpStatus(200)
                        .status(TestStatus.PASS)
                        .durationMs(1)
                        .build(),
                TestCase.builder()
                        .id("support-run-listResources")
                        .runId("support-run")
                        .api("listResources")
                        .name("listResources case")
                        .endpoint("/cats/orgB/support/listResources")
                        .httpMethod("POST")
                        .httpStatus(200)
                        .status(TestStatus.PASS)
                        .durationMs(1)
                        .build()
        ));

        assertThat(queryService.findAvailableApis("orgB", "support"))
                .containsExactly("createTicket", "listDevices");
    }

    @Test
    void findAvailableHttpStatuses_케이스의HttpStatus를정렬해반환한다() {
        storageService.saveRun(run("full-run", null, 1, TestSource.CATS, 1, 1));
        storageService.saveCases("full-run", List.of(
                testCase("full-run", "createReservation", 404, TestStatus.FAIL),
                testCase("full-run", "listResources", 200, TestStatus.PASS)
        ));

        assertThat(queryService.findAvailableHttpStatuses("catsOrg", "booking"))
                .containsExactly(200, 404);
    }

    private TestRun run(String runId, String api, int dayOfMonth, TestSource source, int passCount, int failCount) {
        return TestRun.builder()
                .id(runId)
                .org("catsOrg")
                .service("booking")
                .api(api)
                .source(source)
                .startedAt(LocalDateTime.of(2026, 4, dayOfMonth, 10, 0))
                .durationMs(10)
                .totalCount(passCount + failCount)
                .passCount(passCount)
                .failCount(failCount)
                .reportPath("/reports/" + runId + "/report/index.html")
                .build();
    }

    private TestCase testCase(String runId, String api, int httpStatus, TestStatus status) {
        return TestCase.builder()
                .id(runId + "-" + api + "-" + httpStatus)
                .runId(runId)
                .api(api)
                .name(api + " case")
                .endpoint("/cats/catsOrg/booking/" + api)
                .httpMethod("POST")
                .httpStatus(httpStatus)
                .status(status)
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
                  - id: gw-orgb-support
                    org: orgB
                    service: support
                    openapiPath: orgB-support-gw-openapi.yaml
                """);
        writeContract(baseDir.resolve("cats-gw-openapi.yaml"), "catsOrg", "booking",
                List.of("createReservation", "getResourceDetail", "listResources"));
        writeContract(baseDir.resolve("orgB-support-gw-openapi.yaml"), "orgB", "support",
                List.of("listDevices", "createTicket"));
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
