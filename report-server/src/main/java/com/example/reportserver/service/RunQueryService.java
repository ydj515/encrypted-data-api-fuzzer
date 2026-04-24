package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.service.dto.RunFilter;
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
        return runStorageService.findCases(run.getId()).stream().anyMatch(c -> matchesCase(c, f));
    }

    private boolean matchesCase(TestCase c, RunFilter f) {
        if (hasValue(f.getApi()) && !f.getApi().equals(c.getApi())) return false;
        if (f.getHttpStatus() != null && f.getHttpStatus() != c.getHttpStatus()) return false;
        if (hasValue(f.getCaseName()) && !containsIgnoreCase(c.getName(), f.getCaseName())) return false;
        if (hasValue(f.getEndpoint()) && !containsIgnoreCase(c.getEndpoint(), f.getEndpoint())) return false;
        return true;
    }

    private boolean hasCaseLevelFilter(RunFilter filter) {
        return hasValue(filter.getApi())
                || filter.getHttpStatus() != null
                || hasValue(filter.getCaseName())
                || hasValue(filter.getEndpoint());
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isAllowedApi(String api, Set<String> declaredApis) {
        return hasValue(api) && (declaredApis == null || declaredApis.contains(api));
    }
}
