package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestCaseKind;
import com.example.reportserver.model.TestCaseType;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.service.dto.CaseFilter;
import com.example.reportserver.service.dto.RunFilter;
import com.example.reportserver.service.dto.RunHistoryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class RunQueryService {

    private final RunStorageService runStorageService;
    private final GatewayContractCatalogService gatewayContractCatalogService;

    public List<TestRun> findRuns(RunFilter filter) {
        return runStorageService.listAllRuns().stream()
                .filter(run -> matchesRunFilter(run, filter))
                .filter(run -> matchesCaseFilter(run, filter))
                .toList();
    }

    public Optional<TestRun> findRun(String runId) {
        return runStorageService.findRun(runId);
    }

    public List<TestCase> findCases(String runId) {
        return runStorageService.findCases(runId);
    }

    public List<RunHistoryRow> findHistoryRows(String org, String service) {
        return findHistoryRows(RunFilter.builder().org(org).service(service).build());
    }

    public List<RunHistoryRow> findHistoryRows(RunFilter filter) {
        return findHistoryRows(filter, requiresHistoryCaseMetadata(filter));
    }

    public List<RunHistoryRow> findHistoryRowsWithCaseMetadata(RunFilter filter) {
        return findHistoryRows(filter, true);
    }

    public boolean requiresHistoryCaseMetadata(RunFilter filter) {
        return hasCaseLevelFilter(filter);
    }

    private List<RunHistoryRow> findHistoryRows(RunFilter filter, boolean includeCaseMetadata) {
        return runStorageService.listAllRuns().stream()
                .filter(run -> matches(filter.getOrg(), run.getOrg()) && matches(filter.getService(), run.getService()))
                .map(run -> {
                    List<TestCase> cases = includeCaseMetadata
                            ? runStorageService.findCases(run.getId())
                            : List.of();
                    return toHistoryRow(run, cases, matchesRunFilter(run, filter) && matchesCaseFilter(run, cases, filter));
                })
                .toList();
    }

    public List<TestCase> findScenarioCases(String runId, CaseFilter filter) {
        return runStorageService.findCases(runId).stream()
                .filter(this::isScenarioCase)
                .filter(testCase -> matchesCase(testCase, filter))
                .toList();
    }

    public List<TestCase> findHttpCallCases(String runId, CaseFilter filter) {
        return runStorageService.findCases(runId).stream()
                .filter(tc -> tc.getCaseType() == TestCaseType.HTTP_CALL)
                .filter(tc -> matchesCase(tc, filter))
                .toList();
    }

    public List<String> findAvailableCaseApis(String runId) {
        TreeSet<String> apis = new TreeSet<>();
        runStorageService.findCases(runId).stream()
                .filter(this::isScenarioCase)
                .map(TestCase::getApi)
                .filter(this::hasValue)
                .forEach(apis::add);
        return List.copyOf(apis);
    }

    public List<String> findAvailableHttpCallApis(String runId) {
        TreeSet<String> apis = new TreeSet<>();
        runStorageService.findCases(runId).stream()
                .filter(tc -> tc.getCaseType() == TestCaseType.HTTP_CALL)
                .map(TestCase::getApi)
                .filter(this::hasValue)
                .forEach(apis::add);
        return List.copyOf(apis);
    }

    public List<TestCaseKind> findAvailableCaseKinds(String runId) {
        TreeSet<TestCaseKind> kinds = new TreeSet<>((left, right) -> left.name().compareTo(right.name()));
        runStorageService.findCases(runId).stream()
                .filter(this::isScenarioCase)
                .map(TestCase::getKind)
                .filter(kind -> kind != null)
                .forEach(kinds::add);
        return List.copyOf(kinds);
    }

    public List<String> findAvailableApis(String org, String service) {
        TreeSet<String> apis = new TreeSet<>();
        Set<String> declaredApis = gatewayContractCatalogService.findByOrgService(org, service)
                .map(GatewayContract::getApis)
                .map(HashSet::new)
                .orElse(null);
        if (declaredApis != null) {
            apis.addAll(declaredApis);
        }
        runStorageService.listAllRuns().stream()
                .filter(r -> matches(org, r.getOrg()) && matches(service, r.getService()))
                .forEach(run -> {
                    if (isAllowedApi(run.getApi(), declaredApis)) {
                        apis.add(run.getApi());
                    }
                    runStorageService.findCases(run.getId()).stream()
                            .map(TestCase::getApi)
                            .filter(api -> isAllowedApi(api, declaredApis))
                            .forEach(apis::add);
                });
        return List.copyOf(apis);
    }

    public List<Integer> findAvailableHttpStatuses(String org, String service) {
        TreeSet<Integer> httpStatuses = new TreeSet<>();
        runStorageService.listAllRuns().stream()
                .filter(r -> matches(org, r.getOrg()) && matches(service, r.getService()))
                .forEach(run -> runStorageService.findCases(run.getId()).stream()
                        .map(TestCase::getHttpStatus)
                        .filter(status -> status > 0)
                        .forEach(httpStatuses::add));
        return List.copyOf(httpStatuses);
    }

    private boolean matchesRunFilter(TestRun run, RunFilter f) {
        if (!matches(f.getOrg(), run.getOrg())) return false;
        if (!matches(f.getService(), run.getService())) return false;
        if (f.getSource() != null && f.getSource() != run.getSource()) return false;
        if (f.getStatus() != null) {
            TestStatus runStatus = run.getFailCount() > 0 ? TestStatus.FAIL : TestStatus.PASS;
            if (f.getStatus() != runStatus) return false;
        }
        if (f.getFrom() != null && run.getStartedAt() != null && run.getStartedAt().isBefore(f.getFrom())) return false;
        if (f.getTo() != null && run.getStartedAt() != null && run.getStartedAt().isAfter(f.getTo())) return false;
        return true;
    }

    private boolean matchesCaseFilter(TestRun run, RunFilter f) {
        if (!hasCaseLevelFilter(f)) {
            return true;
        }
        if (matchesRunApiOnlyFilter(run, f)) {
            return true;
        }
        return runStorageService.findCases(run.getId()).stream().anyMatch(c -> matchesCase(c, f));
    }

    private boolean matchesCaseFilter(TestRun run, List<TestCase> cases, RunFilter f) {
        if (!hasCaseLevelFilter(f)) {
            return true;
        }
        if (matchesRunApiOnlyFilter(run, f)) {
            return true;
        }
        return cases.stream().anyMatch(c -> matchesCase(c, f));
    }

    private boolean matchesCase(TestCase c, RunFilter f) {
        if (hasValue(f.getApi()) && !f.getApi().equals(c.getApi())) return false;
        if (hasValue(f.getHttpMethod()) && !equalsIgnoreCase(f.getHttpMethod(), c.getHttpMethod())) return false;
        if (f.getHttpStatus() != null && f.getHttpStatus() != c.getHttpStatus()) return false;
        if (hasValue(f.getCaseName()) && !matchesCaseName(c, f.getCaseName())) return false;
        if (hasValue(f.getEndpoint()) && !containsIgnoreCase(c.getEndpoint(), f.getEndpoint())) return false;
        return true;
    }

    private boolean matchesCase(TestCase c, CaseFilter f) {
        if (f == null) {
            return true;
        }
        if (hasValue(f.getApi()) && !containsIgnoreCase(c.getApi(), f.getApi())) return false;
        if (f.getStatus() != null && f.getStatus() != c.getStatus()) return false;
        if (f.getKind() != null && f.getKind() != c.getKind()) return false;
        return true;
    }

    private boolean hasCaseLevelFilter(RunFilter filter) {
        return hasValue(filter.getApi())
                || hasValue(filter.getHttpMethod())
                || filter.getHttpStatus() != null
                || hasValue(filter.getCaseName())
                || hasValue(filter.getEndpoint());
    }

    private boolean hasCaseLevelFilterExceptApi(RunFilter filter) {
        return hasValue(filter.getHttpMethod())
                || filter.getHttpStatus() != null
                || hasValue(filter.getCaseName())
                || hasValue(filter.getEndpoint());
    }

    private boolean matchesRunApiOnlyFilter(TestRun run, RunFilter filter) {
        return hasValue(filter.getApi())
                && !hasCaseLevelFilterExceptApi(filter)
                && filter.getApi().equals(run.getApi());
    }

    private RunHistoryRow toHistoryRow(TestRun run, List<TestCase> cases, boolean visible) {
        TreeSet<String> apis = new TreeSet<>();
        TreeSet<String> caseNames = new TreeSet<>();
        TreeSet<String> endpoints = new TreeSet<>();
        TreeSet<String> httpMethods = new TreeSet<>();
        TreeSet<Integer> httpStatuses = new TreeSet<>();

        if (hasValue(run.getApi())) {
            apis.add(run.getApi());
        }

        cases.forEach(testCase -> {
            if (hasValue(testCase.getApi())) {
                apis.add(testCase.getApi());
            }
            if (hasValue(testCase.getName())) {
                caseNames.add(testCase.getName());
            }
            if (hasValue(testCase.getScenarioName())) {
                caseNames.add(testCase.getScenarioName());
            }
            if (hasValue(testCase.getEndpoint())) {
                endpoints.add(testCase.getEndpoint());
            }
            if (hasValue(testCase.getHttpMethod())) {
                httpMethods.add(testCase.getHttpMethod().toUpperCase(Locale.ROOT));
            }
            if (testCase.getHttpStatus() > 0) {
                httpStatuses.add(testCase.getHttpStatus());
            }
        });

        return RunHistoryRow.builder()
                .run(run)
                .apis(List.copyOf(apis))
                .caseNames(List.copyOf(caseNames))
                .endpoints(List.copyOf(endpoints))
                .httpMethods(List.copyOf(httpMethods))
                .httpStatuses(List.copyOf(httpStatuses))
                .visible(visible)
                .build();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean matchesCaseName(TestCase testCase, String query) {
        return containsIgnoreCase(testCase.getName(), query)
                || containsIgnoreCase(testCase.getScenarioName(), query);
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isScenarioCase(TestCase testCase) {
        return testCase.getCaseType() == TestCaseType.SCENARIO;
    }

    private boolean isAllowedApi(String api, Set<String> declaredApis) {
        return hasValue(api) && (declaredApis == null || declaredApis.contains(api));
    }
}
