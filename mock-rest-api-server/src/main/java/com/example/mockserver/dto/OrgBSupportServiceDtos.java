package com.example.mockserver.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class OrgBSupportServiceDtos {

    public record DeviceSummary(String deviceId, String name, String model, String status) {
    }

    public record DeviceListResponse(List<DeviceSummary> items, int page, int size, long total) {
    }

    public record SupportTicketCreateRequest(String deviceId,
                                             String requesterId,
                                             String issueType,
                                             String description) {
    }

    public record SupportTicketCreateResponse(String ticketId, String status, OffsetDateTime createdAt) {
    }

    public record SupportResolveRequest(String resolutionNote) {
    }

    public record SupportResolveResponse(String ticketId, String status, OffsetDateTime resolvedAt) {
    }

    public record SupportTicketDetailResponse(String ticketId,
                                              String deviceId,
                                              String requesterId,
                                              String issueType,
                                              String description,
                                              String resolutionNote,
                                              String status,
                                              OffsetDateTime createdAt,
                                              OffsetDateTime resolvedAt) {
    }
}
