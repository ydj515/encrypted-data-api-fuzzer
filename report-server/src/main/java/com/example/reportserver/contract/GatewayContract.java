package com.example.reportserver.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class GatewayContract {
    String id;
    String title;
    String org;
    String service;
    String contractPath;
    String checksum;
    List<String> apis;
    Map<String, String> operationIdsByApi;

    public String findOperationId(String api) {
        if (api == null || api.isBlank() || operationIdsByApi == null) {
            return null;
        }
        return operationIdsByApi.get(api);
    }
}
