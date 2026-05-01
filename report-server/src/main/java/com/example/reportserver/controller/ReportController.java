package com.example.reportserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.service.RunQueryService;
import com.example.reportserver.service.ServiceSummaryService;
import com.example.reportserver.service.dto.CaseFilter;
import com.example.reportserver.service.dto.RunFilter;
import com.example.reportserver.service.dto.RunHistoryRow;
import com.example.reportserver.service.dto.ServiceSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter DATETIME_LOCAL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ServiceSummaryService serviceSummaryService;
    private final RunQueryService runQueryService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String index(Model model) {
        List<ServiceSummary> services = serviceSummaryService.listServiceSummaries();
        model.addAttribute("services", services);
        return "index";
    }

    @GetMapping("/api/services")
    @ResponseBody
    public ResponseEntity<List<ServiceSummary>> apiServices() {
        return ResponseEntity.ok(serviceSummaryService.listServiceSummaries());
    }

    @GetMapping("/api/services/{org}/{service}/history-metadata")
    @ResponseBody
    public ResponseEntity<List<RunHistoryRow>> apiHistoryMetadata(
            @PathVariable String org,
            @PathVariable String service,
            @RequestParam(required = false) String api,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) TestSource source,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to
    ) {
        RunFilter filter = buildRunFilter(
                org, service, api, method, source, status, httpStatus, caseName, endpoint, from, to
        );
        return ResponseEntity.ok(runQueryService.findHistoryRowsWithCaseMetadata(filter));
    }

    @GetMapping("/services/{org}/{service}")
    public String history(
            @PathVariable String org,
            @PathVariable String service,
            @RequestParam(required = false) String api,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) TestSource source,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to,
            Model model
    ) {
        RunFilter filter = buildRunFilter(
                org, service, api, method, source, status, httpStatus, caseName, endpoint, from, to
        );

        List<RunHistoryRow> historyRows = runQueryService.findHistoryRows(filter);
        long visibleHistoryCount = historyRows.stream().filter(RunHistoryRow::isVisible).count();

        model.addAttribute("historyRows", historyRows);
        model.addAttribute("historyPageDataJson", writeJson(historyRows));
        model.addAttribute("visibleHistoryCount", visibleHistoryCount);
        model.addAttribute("historyMetadataComplete", runQueryService.requiresHistoryCaseMetadata(filter));
        model.addAttribute("org", org);
        model.addAttribute("service", service);
        model.addAttribute("filter", filter);
        model.addAttribute("allSources", TestSource.values());
        model.addAttribute("allStatuses", TestStatus.values());
        model.addAttribute("fromStr", from != null ? from.format(DATETIME_LOCAL_FORMAT) : "");
        model.addAttribute("toStr", to != null ? to.format(DATETIME_LOCAL_FORMAT) : "");
        return "history";
    }

    @GetMapping("/api/runs")
    @ResponseBody
    public ResponseEntity<List<TestRun>> apiRuns(
            @RequestParam(required = false) String org,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String api,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) TestSource source,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to
    ) {
        RunFilter filter = buildRunFilter(
                org, service, api, method, source, status, httpStatus, caseName, endpoint, from, to
        );
        return ResponseEntity.ok(runQueryService.findRuns(filter));
    }

    @GetMapping("/runs/{runId}")
    public String runDetail(@PathVariable String runId, Model model) {
        TestRun run = runQueryService.findRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));

        boolean isCats = run.getSource() == TestSource.CATS;
        List<TestCase> cases = isCats
                ? runQueryService.findHttpCallCases(runId, CaseFilter.builder().build())
                : runQueryService.findScenarioCases(runId, CaseFilter.builder().build());
        List<String> availableApis = isCats
                ? runQueryService.findAvailableHttpCallApis(runId)
                : runQueryService.findAvailableCaseApis(runId);

        model.addAttribute("run", run);
        model.addAttribute("cases", cases);
        model.addAttribute("availableApis", availableApis);
        model.addAttribute("availableApisJson", writeJson(availableApis));
        model.addAttribute("availableKinds", isCats ? List.of() : runQueryService.findAvailableCaseKinds(runId));
        model.addAttribute("allStatuses", TestStatus.values());
        return "detail";
    }

    @GetMapping("/reports/{runId}")
    public String rawReportRedirect(@PathVariable String runId) {
        TestRun run = runQueryService.findRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));
        if (run.getReportPath() == null || run.getReportPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report path not found: " + runId);
        }
        return "redirect:" + run.getReportPath();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value)
                    .replace("<", "\\u003c")
                    .replace(">", "\\u003e")
                    .replace("&", "\\u0026")
                    .replace("\u2028", "\\u2028")
                    .replace("\u2029", "\\u2029");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize view model", e);
        }
    }

    private RunFilter buildRunFilter(
            String org,
            String service,
            String api,
            String method,
            TestSource source,
            TestStatus status,
            Integer httpStatus,
            String caseName,
            String endpoint,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return RunFilter.builder()
                .org(org).service(service).api(api).httpMethod(method).source(source).status(status)
                .httpStatus(httpStatus).caseName(caseName).endpoint(endpoint)
                .from(from).to(to)
                .build();
    }
}
