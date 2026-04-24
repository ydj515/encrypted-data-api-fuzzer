package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.contract.GatewayContract;
import com.example.reportserver.service.dto.ServiceSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ServiceSummaryService {

    private final RunStorageService runStorageService;
    private final GatewayContractCatalogService gatewayContractCatalogService;

    public List<ServiceSummary> listServiceSummaries() {
        List<TestRun> allRuns = runStorageService.listAllRuns();
        Map<String, GatewayContract> contractsByKey = new LinkedHashMap<>();
        for (GatewayContract contract : gatewayContractCatalogService.listContracts()) {
            contractsByKey.put(serviceKey(contract.getOrg(), contract.getService()), contract);
        }

        Map<String, List<TestRun>> grouped = new LinkedHashMap<>();
        for (TestRun run : allRuns) {
            String key = serviceKey(run.getOrg(), run.getService());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(run);
        }

        List<ServiceSummary> summaries = new ArrayList<>();
        List<String> serviceKeys = new ArrayList<>(contractsByKey.keySet());
        grouped.keySet().stream()
                .filter(key -> !contractsByKey.containsKey(key))
                .forEach(serviceKeys::add);

        for (String key : serviceKeys) {
            GatewayContract contract = contractsByKey.get(key);
            List<TestRun> runs = grouped.getOrDefault(key, List.of());

            TestRun latest = runs.stream()
                    .max(Comparator.comparing(
                            TestRun::getStartedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    ))
                    .orElse(null);

            TreeSet<String> apiSet = new TreeSet<>();
            Set<String> declaredApis = contract != null ? new HashSet<>(contract.getApis()) : null;
            if (contract != null) {
                apiSet.addAll(contract.getApis());
            }
            for (TestRun run : runs) {
                if (isAllowedApi(run.getApi(), declaredApis)) {
                    apiSet.add(run.getApi());
                }
                runStorageService.findCases(run.getId()).stream()
                        .map(TestCase::getApi)
                        .filter(api -> isAllowedApi(api, declaredApis))
                        .forEach(apiSet::add);
            }
            List<String> apis = new ArrayList<>(apiSet);

            String org = contract != null ? contract.getOrg() : latest.getOrg();
            String service = contract != null ? contract.getService() : latest.getService();
            TestStatus lastStatus = latest == null ? null : (latest.getFailCount() > 0 ? TestStatus.FAIL : TestStatus.PASS);

            summaries.add(ServiceSummary.builder()
                    .contractId(contract != null ? contract.getId() : null)
                    .org(org)
                    .service(service)
                    .apis(apis)
                    .apiCount(apis.size())
                    .lastRunAt(latest != null ? latest.getStartedAt() : null)
                    .lastStatus(lastStatus)
                    .lastRunId(latest != null ? latest.getId() : null)
                    .build());
        }

        summaries.sort(Comparator.comparing(
                ServiceSummary::getLastRunAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(ServiceSummary::getOrg).thenComparing(ServiceSummary::getService));

        return summaries;
    }

    private String serviceKey(String org, String service) {
        return org + "/" + service;
    }

    private boolean isAllowedApi(String api, Set<String> declaredApis) {
        if (api == null || api.isBlank()) {
            return false;
        }
        return declaredApis == null || declaredApis.contains(api);
    }
}
