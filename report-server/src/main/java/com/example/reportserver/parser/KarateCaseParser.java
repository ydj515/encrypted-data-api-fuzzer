package com.example.reportserver.parser;

import com.example.reportserver.model.TestCaseGranularity;
import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestCaseType;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.parser.dto.KarateFeatureResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class KarateCaseParser {

    private static final Pattern HTTP_REQUEST_PATTERN =
            Pattern.compile("\\d+ > ([A-Z]+) https?://[^/]+(/[^\\n\\s]*)");
    private static final Pattern HTTP_STATUS_PATTERN =
            Pattern.compile("\\d+ < (\\d{3})");

    private final ObjectMapper objectMapper;

    public List<TestCase> parse(Path reportDir, String runId, TestCaseGranularity granularity) {
        try (Stream<Path> files = Files.list(reportDir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".karate-json.txt"))
                    .flatMap(p -> parseFeature(p, runId, granularity))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<TestCase> parseFeature(Path featureFile, String runId, TestCaseGranularity granularity) {
        try {
            KarateFeatureResult feature = objectMapper.readValue(featureFile.toFile(), KarateFeatureResult.class);
            if (feature.getScenarioResults() == null) {
                return Stream.empty();
            }
            return feature.getScenarioResults().stream()
                    .flatMap(scenario -> buildTestCases(scenario, runId, granularity).stream());
        } catch (IOException e) {
            log.warn("feature JSON 파싱 실패, 스킵: {}", featureFile, e);
            return Stream.empty();
        }
    }

    private List<TestCase> buildTestCases(
            KarateFeatureResult.ScenarioResult scenario,
            String runId,
            TestCaseGranularity granularity
    ) {
        List<TestCase> cases = new ArrayList<>();
        List<HttpCall> httpCalls = extractHttpCalls(scenario.getStepResults());

        if (granularity.includesScenario()) {
            cases.add(buildScenarioTestCase(scenario, runId, httpCalls));
        }
        if (granularity.includesHttpCall()) {
            cases.addAll(buildHttpCallTestCases(scenario, runId, httpCalls));
        }

        return cases;
    }

    private TestCase buildScenarioTestCase(
            KarateFeatureResult.ScenarioResult scenario,
            String runId,
            List<HttpCall> httpCalls
    ) {
        String api = extractApi(scenario.getTags());
        String failureMsg = scenario.isFailed() ? extractFailureMsg(scenario.getStepResults()) : null;
        HttpInfo httpInfo = summarizeHttpCalls(httpCalls);

        return TestCase.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .caseType(TestCaseType.SCENARIO)
                .api(api)
                .name(scenario.getName())
                .scenarioName(scenario.getName())
                .sequence(0)
                .endpoint(httpInfo.endpoint())
                .httpMethod(httpInfo.method())
                .httpStatus(httpInfo.status())
                .status(scenario.isFailed() ? TestStatus.FAIL : TestStatus.PASS)
                .durationMs((long) scenario.getDurationMillis())
                .failureMsg(failureMsg)
                .build();
    }

    private List<TestCase> buildHttpCallTestCases(
            KarateFeatureResult.ScenarioResult scenario,
            String runId,
            List<HttpCall> httpCalls
    ) {
        String api = extractApi(scenario.getTags());
        List<TestCase> cases = new ArrayList<>();

        for (int i = 0; i < httpCalls.size(); i++) {
            HttpCall httpCall = httpCalls.get(i);
            int sequence = i + 1;
            cases.add(TestCase.builder()
                    .id(UUID.randomUUID().toString())
                    .runId(runId)
                    .caseType(TestCaseType.HTTP_CALL)
                    .api(api)
                    .name(scenario.getName() + " #" + sequence)
                    .scenarioName(scenario.getName())
                    .sequence(sequence)
                    .endpoint(httpCall.info().endpoint())
                    .httpMethod(httpCall.info().method())
                    .httpStatus(httpCall.info().status())
                    .status(httpCall.failed() ? TestStatus.FAIL : TestStatus.PASS)
                    .durationMs((long) httpCall.durationMillis())
                    .failureMsg(httpCall.failureMsg())
                    .build());
        }

        return cases;
    }

    private String extractApi(List<String> tags) {
        if (tags == null) return null;
        return tags.stream()
                .filter(t -> t.startsWith("api=") || t.startsWith("@api="))
                .map(t -> t.startsWith("@") ? t.substring(5) : t.substring(4))
                .findFirst()
                .orElse(null);
    }

    private List<HttpCall> extractHttpCalls(List<KarateFeatureResult.StepResult> stepResults) {
        if (stepResults == null) return List.of();

        List<HttpCall> calls = new ArrayList<>();
        PendingCall pendingCall = null;

        for (KarateFeatureResult.StepResult step : stepResults) {
            HttpInfo httpInfo = extractHttpInfo(step);
            if (httpInfo != null) {
                if (pendingCall != null) {
                    calls.add(pendingCall.toHttpCall());
                }
                pendingCall = new PendingCall(httpInfo, stepDuration(step));
                pendingCall.addStep(step);
                continue;
            }

            if (pendingCall != null) {
                pendingCall.addStep(step);
            }
        }

        if (pendingCall != null) {
            calls.add(pendingCall.toHttpCall());
        }

        return calls;
    }

    private HttpInfo extractHttpInfo(KarateFeatureResult.StepResult step) {
        if (step == null || step.getStepLog() == null) return null;

        String log = step.getStepLog();
        Matcher reqMatcher = HTTP_REQUEST_PATTERN.matcher(log);
        Matcher statusMatcher = HTTP_STATUS_PATTERN.matcher(log);
        if (reqMatcher.find() && statusMatcher.find()) {
            return new HttpInfo(reqMatcher.group(1), reqMatcher.group(2), Integer.parseInt(statusMatcher.group(1)));
        }
        return null;
    }

    private HttpInfo summarizeHttpCalls(List<HttpCall> httpCalls) {
        if (httpCalls.isEmpty()) {
            return HttpInfo.unknown();
        }
        if (httpCalls.size() == 1) {
            return httpCalls.getFirst().info();
        }
        return new HttpInfo("MULTIPLE", "MULTIPLE", 0);
    }

    private String extractFailureMsg(List<KarateFeatureResult.StepResult> stepResults) {
        if (stepResults == null) return null;
        return stepResults.stream()
                .filter(s -> s.getResult() != null && "failed".equals(s.getResult().getStatus()))
                .map(s -> s.getResult().getError())
                .filter(e -> e != null && !e.isBlank())
                .findFirst()
                .orElse(null);
    }

    private double stepDuration(KarateFeatureResult.StepResult step) {
        if (step == null || step.getResult() == null) {
            return 0;
        }
        return step.getResult().getMillis();
    }

    private boolean isFailed(KarateFeatureResult.StepResult step) {
        return step != null
                && step.getResult() != null
                && "failed".equals(step.getResult().getStatus());
    }

    private String failureMsg(KarateFeatureResult.StepResult step) {
        if (step == null || step.getResult() == null) {
            return null;
        }
        String error = step.getResult().getError();
        return error == null || error.isBlank() ? null : error;
    }

    private record HttpInfo(String method, String endpoint, int status) {
        static HttpInfo unknown() {
            return new HttpInfo("UNKNOWN", "UNKNOWN", 0);
        }
    }

    private record HttpCall(HttpInfo info, double durationMillis, boolean failed, String failureMsg) {
    }

    private final class PendingCall {

        private final HttpInfo info;
        private double durationMillis;
        private boolean failed;
        private String failureMsg;

        private PendingCall(HttpInfo info, double durationMillis) {
            this.info = info;
            this.durationMillis = durationMillis;
        }

        private void addStep(KarateFeatureResult.StepResult step) {
            if (isFailed(step)) {
                failed = true;
                if (failureMsg == null) {
                    failureMsg = failureMsg(step);
                }
            }
        }

        private HttpCall toHttpCall() {
            return new HttpCall(info, durationMillis, failed, failureMsg);
        }
    }
}
