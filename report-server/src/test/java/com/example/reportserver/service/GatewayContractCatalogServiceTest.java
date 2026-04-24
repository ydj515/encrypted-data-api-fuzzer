package com.example.reportserver.service;

import com.example.reportserver.contract.GatewayContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayContractCatalogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listContracts_카탈로그와OpenApi를함께해석한다() throws Exception {
        Path contractsDir = tempDir.resolve("gateway");
        Files.createDirectories(contractsDir);
        Files.writeString(contractsDir.resolve("catalog.yaml"), """
                version: 1
                contracts:
                  - id: gw-orgb-visit
                    org: orgB
                    service: visit
                    openapiPath: orgB-visit-gw-openapi.yaml
                """);
        writeContract(
                contractsDir.resolve("orgB-visit-gw-openapi.yaml"),
                "orgB",
                "visit",
                Map.of(
                        "listSites", "listSitesViaGateway",
                        "createVisit", "createVisitViaGateway"
                )
        );

        GatewayContractCatalogService catalogService =
                new GatewayContractCatalogService(contractsDir.resolve("catalog.yaml"));

        GatewayContract contract = catalogService.listContracts().getFirst();

        assertThat(contract.getId()).isEqualTo("gw-orgb-visit");
        assertThat(contract.getOrg()).isEqualTo("orgB");
        assertThat(contract.getService()).isEqualTo("visit");
        assertThat(contract.getApis()).containsExactly("createVisit", "listSites");
        assertThat(contract.findOperationId("createVisit")).isEqualTo("createVisitViaGateway");
        assertThat(contract.getChecksum()).hasSize(64);
    }

    private void writeContract(Path path, String org, String service, Map<String, String> operations) throws Exception {
        StringBuilder pathsYaml = new StringBuilder();
        operations.forEach((api, operationId) -> pathsYaml.append("""
                  /cats/{org}/{service}/%s:
                    post:
                      operationId: %s
                """.formatted(api, operationId)));

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
                """.formatted(org, service, pathsYaml, org, org, service, service));
    }
}
