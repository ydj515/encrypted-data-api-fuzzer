package com.example.reportserver.controller;

import com.example.reportserver.controller.dto.KaratePublishRequest;
import com.example.reportserver.model.TestCaseGranularity;
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

    @PostMapping("/karate")
    public ResponseEntity<String> publishKarate(@RequestBody KaratePublishRequest request) {
        String runId = runPublishService.publishKarate(
                request.getRunId(),
                Path.of(request.getReportDir()),
                request.getOrg(),
                request.getService(),
                request.getApi(),
                TestCaseGranularity.from(request.getCaseGranularity())
        );
        return ResponseEntity.ok(runId);
    }
}
