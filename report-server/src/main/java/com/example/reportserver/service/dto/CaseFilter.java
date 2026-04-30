package com.example.reportserver.service.dto;

import com.example.reportserver.model.TestCaseKind;
import com.example.reportserver.model.TestStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CaseFilter {
    String api;
    TestStatus status;
    TestCaseKind kind;
}
