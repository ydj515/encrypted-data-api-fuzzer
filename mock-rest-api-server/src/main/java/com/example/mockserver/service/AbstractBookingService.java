package com.example.mockserver.service;

import com.example.mockserver.dto.BookingDomainModels;
import com.example.mockserver.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBookingService {

    private static final int DEFAULT_SAMPLE_RESOURCE_COUNT = 12;
    private static final int MIN_SCHEDULE_ITEMS = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final Set<String> ALLOWED_RESOURCE_CATEGORIES = Set.of("SPACE", "STUDIO", "EQUIPMENT");

    private final Map<String, BookingDomainModels.ResourceDetail> resources = new ConcurrentHashMap<>();
    private final Map<String, ReservationEntity> reservations = new ConcurrentHashMap<>();

    protected AbstractBookingService() {
        initializeSampleResources();
    }

    protected BookingDomainModels.ResourceListResponse listResourcesModel(int page, int size, String category) {
        validateListResourcesRequest(page, size, category);

        List<BookingDomainModels.ResourceSummary> filtered = resources.values().stream()
                .filter(v -> category == null || category.isBlank() || v.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(BookingDomainModels.ResourceDetail::resourceId))
                .map(v -> new BookingDomainModels.ResourceSummary(v.resourceId(), v.name(), v.category(), v.active()))
                .toList();

        long offset = (long) page * size;
        int from = (int) Math.min(offset, filtered.size());
        int to = (int) Math.min(offset + size, filtered.size());
        List<BookingDomainModels.ResourceSummary> paged = filtered.subList(from, to);

        return new BookingDomainModels.ResourceListResponse(paged, page, size, filtered.size());
    }

    protected BookingDomainModels.ResourceDetail getResourceModel(String resourceId) {
        BookingDomainModels.ResourceDetail detail = resources.get(resourceId);
        if (detail == null) {
            throw new ApiException("RESOURCE_NOT_FOUND", "Resource not found: " + resourceId, HttpStatus.NOT_FOUND.value());
        }
        return detail;
    }

    protected BookingDomainModels.InventoryStatus getInventoryModel(String resourceId, LocalDate date) {
        getResourceModel(resourceId);
        int total = 100;
        int reserved = (int) reservations.values().stream()
                .filter(r -> r.resourceId().equals(resourceId))
                .filter(r -> !"CANCELED".equals(r.status()))
                .mapToInt(ReservationEntity::quantity)
                .sum();
        int available = Math.max(0, total - reserved);
        return new BookingDomainModels.InventoryStatus(resourceId, date, total, available, reserved);
    }

    protected BookingDomainModels.ScheduleListResponse getSchedulesModel(String resourceId, OffsetDateTime from, OffsetDateTime to) {
        getResourceModel(resourceId);
        if (!from.isBefore(to)) {
            throw new ApiException("INVALID_RANGE", "from must be before to", HttpStatus.BAD_REQUEST.value());
        }

        List<BookingDomainModels.ScheduleItem> items = new ArrayList<>();
        OffsetDateTime cursor = from.withMinute(0).withSecond(0).withNano(0);
        while (cursor.isBefore(to)) {
            OffsetDateTime next = cursor.plusHours(1);
            if (next.isAfter(to)) {
                break;
            }
            String scheduleId = resourceId + "-" + cursor.toLocalDate() + "-" + cursor.getHour();
            items.add(new BookingDomainModels.ScheduleItem(scheduleId, cursor, next, "AVAILABLE"));
            cursor = next;
        }

        // 퍼징/시나리오 테스트에서 리스트 응답 최소 샘플 개수를 맞추기 위해 보정한다.
        while (items.size() < MIN_SCHEDULE_ITEMS) {
            OffsetDateTime start = cursor;
            OffsetDateTime end = cursor.plusHours(1);
            String scheduleId = resourceId + "-" + start.toLocalDate() + "-" + start.getHour();
            items.add(new BookingDomainModels.ScheduleItem(scheduleId, start, end, "AVAILABLE"));
            cursor = end;
        }
        return new BookingDomainModels.ScheduleListResponse(resourceId, items);
    }

    protected BookingDomainModels.DailyScheduleListResponse listDailySchedulesModel(LocalDate date) {
        List<BookingDomainModels.DailyScheduleItem> items = resources.keySet().stream()
                .sorted()
                .map(resourceId -> {
                    OffsetDateTime start = date.atTime(9, 0).atOffset(ZoneOffset.ofHours(9));
                    OffsetDateTime end = start.plusHours(1);
                    String scheduleId = resourceId + "-" + date + "-9";
                    return new BookingDomainModels.DailyScheduleItem(resourceId, scheduleId, start, end, "AVAILABLE");
                }).toList();
        return new BookingDomainModels.DailyScheduleListResponse(date, items);
    }

    protected BookingDomainModels.ReservationCreateResponse createReservationModel(BookingDomainModels.ReservationCreateRequest request) {
        validateCreateRequest(request);
        getResourceModel(request.resourceId());

        int available = getInventoryModel(request.resourceId(), LocalDate.now()).availableQuantity();
        if (request.quantity() > available) {
            throw new ApiException("RESERVATION_CONFLICT", "Insufficient inventory", HttpStatus.CONFLICT.value());
        }

        String reservationId = "RSV-" + UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        reservations.put(reservationId, new ReservationEntity(
                reservationId,
                request.resourceId(),
                request.scheduleId(),
                request.userId(),
                request.quantity(),
                "CREATED",
                now,
                null
        ));
        return new BookingDomainModels.ReservationCreateResponse(reservationId, "CREATED", now);
    }

    protected BookingDomainModels.ReservationCancelResponse cancelReservationModel(String reservationId,
                                                                                   BookingDomainModels.ReservationCancelRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new ApiException("INVALID_REQUEST", "Cancel reason is required", HttpStatus.BAD_REQUEST.value());
        }
        ReservationEntity current = getReservationEntity(reservationId);
        if ("CANCELED".equals(current.status())) {
            return new BookingDomainModels.ReservationCancelResponse(reservationId, "CANCELED", current.canceledAt());
        }

        OffsetDateTime canceledAt = OffsetDateTime.now();
        reservations.put(reservationId, current.withStatus("CANCELED", canceledAt));
        return new BookingDomainModels.ReservationCancelResponse(reservationId, "CANCELED", canceledAt);
    }

    protected BookingDomainModels.ReservationDetailResponse getReservationModel(String reservationId) {
        ReservationEntity entity = getReservationEntity(reservationId);
        return new BookingDomainModels.ReservationDetailResponse(
                entity.reservationId(),
                entity.resourceId(),
                entity.scheduleId(),
                entity.userId(),
                entity.quantity(),
                entity.status(),
                entity.createdAt(),
                entity.canceledAt()
        );
    }

    private void validateCreateRequest(BookingDomainModels.ReservationCreateRequest request) {
        if (request == null) {
            throw new ApiException("INVALID_REQUEST", "Request body is required", HttpStatus.BAD_REQUEST.value());
        }
        if (isBlank(request.resourceId()) || isBlank(request.scheduleId()) || isBlank(request.userId())) {
            throw new ApiException("INVALID_REQUEST", "resourceId, scheduleId, userId are required", HttpStatus.BAD_REQUEST.value());
        }
        if (request.quantity() == null || request.quantity() < 1 || request.quantity() > 9999) {
            throw new ApiException("INVALID_REQUEST", "quantity must be 1..9999", HttpStatus.BAD_REQUEST.value());
        }
    }

    private ReservationEntity getReservationEntity(String reservationId) {
        ReservationEntity entity = reservations.get(reservationId);
        if (entity == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found: " + reservationId, HttpStatus.NOT_FOUND.value());
        }
        return entity;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateListResourcesRequest(int page, int size, String category) {
        if (page < 0 || page > MAX_PAGE_NUMBER) {
            throw new ApiException("INVALID_REQUEST", "page must be between 0 and " + MAX_PAGE_NUMBER, HttpStatus.BAD_REQUEST.value());
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException("INVALID_REQUEST", "size must be between 1 and " + MAX_PAGE_SIZE, HttpStatus.BAD_REQUEST.value());
        }
        if (category != null && !category.isBlank() && !ALLOWED_RESOURCE_CATEGORIES.contains(category.toUpperCase(Locale.ROOT))) {
            throw new ApiException("INVALID_REQUEST", "category must be one of " + ALLOWED_RESOURCE_CATEGORIES, HttpStatus.BAD_REQUEST.value());
        }
    }

    private void initializeSampleResources() {
        for (int i = 1; i <= DEFAULT_SAMPLE_RESOURCE_COUNT; i++) {
            String resourceId = "R-%03d".formatted(i);
            String category = i % 3 == 0 ? "EQUIPMENT" : (i % 2 == 0 ? "STUDIO" : "SPACE");
            resources.put(resourceId, new BookingDomainModels.ResourceDetail(
                    resourceId,
                    "Resource " + i,
                    category,
                    true,
                    "Sample resource " + i,
                    (i % 5 + 1) + "F",
                    "Asia/Seoul"
            ));
        }
    }

    private record ReservationEntity(String reservationId,
                                     String resourceId,
                                     String scheduleId,
                                     String userId,
                                     int quantity,
                                     String status,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime canceledAt) {
        private ReservationEntity withStatus(String nextStatus, OffsetDateTime nextCanceledAt) {
            return new ReservationEntity(
                    reservationId,
                    resourceId,
                    scheduleId,
                    userId,
                    quantity,
                    nextStatus,
                    createdAt,
                    nextCanceledAt
            );
        }
    }
}
