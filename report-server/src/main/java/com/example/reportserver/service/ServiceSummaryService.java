package com.example.reportserver.service;

import com.example.reportserver.model.TestCase;
import com.example.reportserver.model.TestRun;
import com.example.reportserver.model.TestStatus;
import com.example.reportserver.service.dto.ServiceSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ServiceSummaryService {

    private final RunStorageService runStorageService;

    public List<ServiceSummary> listServiceSummaries() {
        List<TestRun> allRuns = runStorageService.listAllRuns();

        // org+service 키로 그룹핑 후 최신 run 순서 유지
        Map<String, List<TestRun>> grouped = new LinkedHashMap<>();
        for (TestRun run : allRuns) {
            String key = run.getOrg() + "/" + run.getService();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(run);
        }

        List<ServiceSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<TestRun>> entry : grouped.entrySet()) {
            List<TestRun> runs = entry.getValue();

            // 가장 최신 run (startedAt 기준)
            TestRun latest = runs.stream()
                    .max(Comparator.comparing(
                            TestRun::getStartedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    ))
                    .orElseThrow();

            // 이력 전체에서 등장한 API 값을 run 메타와 케이스 양쪽에서 수집한다.
            TreeSet<String> apiSet = new TreeSet<>();
            for (TestRun run : runs) {
                if (run.getApi() != null && !run.getApi().isBlank()) {
                    apiSet.add(run.getApi());
                }
                runStorageService.findCases(run.getId()).stream()
                        .map(TestCase::getApi)
                        .filter(api -> api != null && !api.isBlank())
                        .forEach(apiSet::add);
            }
            List<String> apis = new ArrayList<>(apiSet);

            TestStatus lastStatus = latest.getFailCount() > 0 ? TestStatus.FAIL : TestStatus.PASS;

            summaries.add(ServiceSummary.builder()
                    .org(latest.getOrg())
                    .service(latest.getService())
                    .apis(apis)
                    .apiCount(apis.size())
                    .lastRunAt(latest.getStartedAt())
                    .lastStatus(lastStatus)
                    .lastRunId(latest.getId())
                    .build());
        }

        // 서비스 목록은 마지막 수행 시각 내림차순 정렬
        summaries.sort(Comparator.comparing(
                ServiceSummary::getLastRunAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return summaries;
    }
}
