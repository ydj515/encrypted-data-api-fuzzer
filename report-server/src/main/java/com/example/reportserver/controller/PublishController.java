package com.example.reportserver.controller;

import com.example.reportserver.controller.dto.CatsPublishRequest;
import com.example.reportserver.controller.dto.KaratePublishRequest;
import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.service.GatewayContractCatalogService;
import com.example.reportserver.service.RunPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class PublishController {

    private final RunPublishService runPublishService;
    private final GatewayContractCatalogService gatewayContractCatalogService;

    @PostMapping("/karate")
    public ResponseEntity<String> publishKarate(@RequestBody KaratePublishRequest request) {
        GatewayContract contract = resolveContract(
                request.getContractId(),
                request.getContractPath(),
                request.getOrg(),
                request.getService()
        );
        String runId = runPublishService.publishKarate(
                request.getRunId(),
                Path.of(request.getReportDir()),
                contract,
                request.getApi(),
                TestCaseGranularity.from(request.getCaseGranularity())
        );
        return ResponseEntity.ok(runId);
    }

    @PostMapping("/cats")
    public ResponseEntity<String> publishCats(@RequestBody CatsPublishRequest request) {
        GatewayContract contract = resolveContract(
                request.getContractId(),
                request.getContractPath(),
                request.getOrg(),
                request.getService()
        );
        String runId = runPublishService.publishCats(
                request.getRunId(),
                Path.of(request.getReportDir()),
                contract,
                request.getApi()
        );
        return ResponseEntity.ok(runId);
    }

    private GatewayContract resolveContract(
            String contractId,
            String contractPath,
            String org,
            String service
    ) {
        return gatewayContractCatalogService.resolveContract(contractId, contractPath)
                .or(() -> gatewayContractCatalogService.findByOrgService(org, service))
                .orElseThrow(() -> new IllegalArgumentException(
                        "contractId/contractPath 또는 org/service가 필요합니다."
                ));
    }
}
