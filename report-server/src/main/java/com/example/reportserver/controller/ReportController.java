package com.example.reportserver.controller;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.service.RunQueryService;
import com.example.reportserver.service.ServiceSummaryService;
import com.example.reportserver.service.dto.RunFilter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter DATETIME_LOCAL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final List<Integer> COMMON_HTTP_STATUSES =
            List.of(200, 201, 204, 400, 401, 403, 404, 409, 422, 500, 502, 503);

    private final ServiceSummaryService serviceSummaryService;
    private final RunQueryService runQueryService;

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

    @GetMapping("/services/{org}/{service}")
    public String history(
            @PathVariable String org,
            @PathVariable String service,
            @RequestParam(required = false) String api,
            @RequestParam(required = false) TestSource source,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to,
            Model model
    ) {
        RunFilter filter = RunFilter.builder()
                .org(org).service(service).api(api).source(source).status(status)
                .httpStatus(httpStatus).caseName(caseName).endpoint(endpoint)
                .from(from).to(to)
                .build();

        List<TestRun> runs = runQueryService.findRuns(filter);
        List<String> availableApis = runQueryService.findAvailableApis(org, service);
        List<Integer> availableHttpStatuses = availableHttpStatuses(org, service);

        model.addAttribute("runs", runs);
        model.addAttribute("org", org);
        model.addAttribute("service", service);
        model.addAttribute("filter", filter);
        model.addAttribute("availableApis", availableApis);
        model.addAttribute("allSources", TestSource.values());
        model.addAttribute("allStatuses", TestStatus.values());
        model.addAttribute("availableHttpStatuses", availableHttpStatuses);
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
            @RequestParam(required = false) TestSource source,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to
    ) {
        RunFilter filter = RunFilter.builder()
                .org(org).service(service).api(api).source(source).status(status)
                .httpStatus(httpStatus).caseName(caseName).endpoint(endpoint)
                .from(from).to(to)
                .build();
        return ResponseEntity.ok(runQueryService.findRuns(filter));
    }

    @GetMapping("/runs/{runId}")
    public String runDetail(@PathVariable String runId, Model model) {
        TestRun run = runQueryService.findRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));
        List<TestCase> cases = runQueryService.findCases(runId);
        model.addAttribute("run", run);
        model.addAttribute("cases", cases);
        return "detail";
    }

    private List<Integer> availableHttpStatuses(String org, String service) {
        TreeSet<Integer> statuses = new TreeSet<>(COMMON_HTTP_STATUSES);
        statuses.addAll(runQueryService.findAvailableHttpStatuses(org, service));
        return new ArrayList<>(statuses);
    }
}
