package com.example.reportserver.service.dto;

import com.example.reportserver.model.TestRun;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RunHistoryRow {
    TestRun run;
    List<String> apis;
    List<String> caseNames;
    List<String> endpoints;
    List<String> httpMethods;
    List<Integer> httpStatuses;
}
