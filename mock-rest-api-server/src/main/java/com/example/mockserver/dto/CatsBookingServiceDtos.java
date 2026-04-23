package com.example.mockserver.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class CatsBookingServiceDtos {

    public record ResourceSummary(String resourceId, String name, String category, boolean active) {
    }

    public record ResourceDetail(String resourceId,
                                 String name,
                                 String category,
                                 boolean active,
                                 String description,
                                 String location,
                                 String timezone) {
    }

    public record ResourceListResponse(List<ResourceSummary> items, int page, int size, long total) {
    }

    public record InventoryStatus(String resourceId,
                                  LocalDate date,
                                  int totalQuantity,
                                  int availableQuantity,
                                  int reservedQuantity) {
    }

    public record ScheduleItem(String scheduleId, OffsetDateTime startAt, OffsetDateTime endAt, String status) {
    }

    public record ScheduleListResponse(String resourceId, List<ScheduleItem> items) {
    }

    public record DailyScheduleItem(String resourceId,
                                    String scheduleId,
                                    OffsetDateTime startAt,
                                    OffsetDateTime endAt,
                                    String status) {
    }

    public record DailyScheduleListResponse(LocalDate date, List<DailyScheduleItem> items) {
    }

    public record ReservationCreateRequest(String resourceId,
                                           String scheduleId,
                                           String userId,
                                           Integer quantity,
                                           String memo) {
    }

    public record ReservationCreateResponse(String reservationId, String status, OffsetDateTime createdAt) {
    }

    public record ReservationCancelRequest(String reason) {
    }

    public record ReservationCancelResponse(String reservationId, String status, OffsetDateTime canceledAt) {
    }

    public record ReservationDetailResponse(String reservationId,
                                            String resourceId,
                                            String scheduleId,
                                            String userId,
                                            int quantity,
                                            String status,
                                            OffsetDateTime createdAt,
                                            OffsetDateTime canceledAt) {
    }
}
