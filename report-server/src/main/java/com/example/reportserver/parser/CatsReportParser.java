package com.example.reportserver.parser;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestCaseType;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestSource;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.parser.dto.CatsIndividualReport;
import com.example.reportserver.parser.dto.CatsSummaryReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatsReportParser {

    private static final String SUMMARY_FILE = "cats-summary-report.json";
    private static final String REPORT_INDEX = "index.html";

    // cats가 출력하는 타임스탬프 포맷: "Mon, 2 Mar 2026 16:27:16 +0900"
    private static final DateTimeFormatter CATS_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final ObjectMapper objectMapper;

    public TestRun parseRun(
            Path reportDir,
            String runId,
            String org,
            String service,
            String api
    ) {
        CatsSummaryReport summary = readSummary(reportDir);
        List<CatsSummaryReport.TestCaseItem> items = summary.getTestCases() == null
                ? List.of()
                : summary.getTestCases();

        int total = items.size();
        long passCount = items.stream().filter(i -> isPass(i.getResult())).count();
        long failCount = total - passCount;
        long durationMs = items.stream()
                .mapToLong(i -> parseDurationMs(i.getTimeToExecuteInMs()))
                .sum();

        return TestRun.builder()
                .id(runId)
                .org(org)
                .service(service)
                .api(api)
                .source(TestSource.CATS)
                .caseGranularity(null)
                .startedAt(resolveStartedAt(reportDir))
                .durationMs(durationMs)
                .totalCount(total)
                .passCount((int) passCount)
                .failCount((int) failCount)
                .reportPath("/reports/" + runId + "/report/" + REPORT_INDEX)
                .build();
    }

    public List<TestCase> parseCases(Path reportDir, String runId, String org, String service) {
        CatsSummaryReport summary = readSummary(reportDir);
        if (summary.getTestCases() == null) {
            return List.of();
        }

        return summary.getTestCases().stream()
                .map(item -> buildTestCase(item, runId, org, service))
                .toList();
    }

    private TestCase buildTestCase(CatsSummaryReport.TestCaseItem item, String runId, String org, String service) {
        String api = extractApiFromPath(item.getPath());
        String endpoint = resolveEndpoint(item.getPath(), org, service);
        String failureMsg = isPass(item.getResult()) ? null : item.getResultDetails();

        return TestCase.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .caseType(TestCaseType.HTTP_CALL)
                .api(api)
                .name(item.getScenario())
                .scenarioName(item.getFuzzer())
                .sequence(1)
                .endpoint(endpoint)
                .httpMethod(item.getHttpMethod() != null ? item.getHttpMethod().toUpperCase(Locale.ROOT) : "UNKNOWN")
                .httpStatus(item.getHttpResponseCode())
                .status(isPass(item.getResult()) ? TestStatus.PASS : TestStatus.FAIL)
                .durationMs(parseDurationMs(item.getTimeToExecuteInMs()))
                .failureMsg(failureMsg)
                .build();
    }

    private CatsSummaryReport readSummary(Path reportDir) {
        Path summaryFile = reportDir.resolve(SUMMARY_FILE);
        if (!Files.exists(summaryFile)) {
            throw new IllegalArgumentException("cats-summary-report.json 파일이 없습니다: " + summaryFile);
        }
        try {
            return objectMapper.readValue(summaryFile.toFile(), CatsSummaryReport.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LocalDateTime resolveStartedAt(Path reportDir) {
        // 번호가 가장 작은 TestN.json에서 타임스탬프 추출
        try (Stream<Path> files = Files.list(reportDir)) {
            return files
                    .filter(p -> p.getFileName().toString().matches("Test\\d+\\.json"))
                    .min(Comparator.comparingInt(p -> extractTestNumber(p.getFileName().toString())))
                    .flatMap(p -> readTimestamp(p))
                    .orElseGet(LocalDateTime::now);
        } catch (IOException e) {
            log.warn("CATS 리포트 디렉토리 목록 조회 실패, 현재 시각 사용: {}", reportDir, e);
            return LocalDateTime.now();
        }
    }

    private java.util.Optional<LocalDateTime> readTimestamp(Path jsonFile) {
        try {
            CatsIndividualReport report = objectMapper.readValue(jsonFile.toFile(), CatsIndividualReport.class);
            if (report.getRequest() == null || report.getRequest().getTimestamp() == null) {
                return java.util.Optional.empty();
            }
            ZonedDateTime zdt = ZonedDateTime.parse(
                    report.getRequest().getTimestamp(), CATS_TIMESTAMP_FORMATTER);
            return java.util.Optional.of(zdt.toLocalDateTime());
        } catch (IOException | DateTimeParseException e) {
            log.warn("CATS 개별 JSON 타임스탬프 파싱 실패, 스킵: {}", jsonFile, e);
            return java.util.Optional.empty();
        }
    }

    private int extractTestNumber(String filename) {
        try {
            return Integer.parseInt(filename.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private String extractApiFromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] segments = path.split("/");
        String last = segments[segments.length - 1];
        return last.isBlank() ? null : last;
    }

    private String resolveEndpoint(String pathTemplate, String org, String service) {
        if (pathTemplate == null) {
            return "UNKNOWN";
        }
        return pathTemplate
                .replace("{org}", org != null ? org : "")
                .replace("{service}", service != null ? service : "");
    }

    private boolean isPass(String result) {
        // "success"와 "warn"은 PASS로 처리 (경고는 실패가 아님)
        return "success".equalsIgnoreCase(result) || "warn".equalsIgnoreCase(result);
    }

    private long parseDurationMs(String timeToExecuteInMs) {
        if (timeToExecuteInMs == null || timeToExecuteInMs.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(timeToExecuteInMs.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
