package com.example.mockserver.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class OrgBVisitServiceDtos {

    public record SiteSummary(String siteId, String name, String city, boolean active) {
    }

    public record SiteListResponse(List<SiteSummary> items, int page, int size, long total) {
    }

    public record VisitSlotStatus(String siteId, LocalDate date, int totalSlots, int availableSlots, int reservedSlots) {
    }

    public record VisitCreateRequest(String siteId,
                                     LocalDate visitDate,
                                     String visitorName,
                                     String visitorPhone,
                                     Integer partySize,
                                     String purpose) {
    }

    public record VisitCreateResponse(String visitId, String status, OffsetDateTime createdAt) {
    }

    public record VisitCancelRequest(String reason) {
    }

    public record VisitCancelResponse(String visitId, String status, OffsetDateTime canceledAt) {
    }

    public record VisitDetailResponse(String visitId,
                                      String siteId,
                                      LocalDate visitDate,
                                      String visitorName,
                                      String visitorPhone,
                                      int partySize,
                                      String purpose,
                                      String status,
                                      OffsetDateTime createdAt,
                                      OffsetDateTime canceledAt) {
    }
}
