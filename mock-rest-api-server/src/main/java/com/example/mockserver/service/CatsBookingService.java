package com.example.mockserver.service;

import com.example.mockserver.dto.BookingDomainModels;
import com.example.mockserver.dto.CatsBookingServiceDtos;
import org.springframework.stereotype.Service;

@Service
public class CatsBookingService extends AbstractBookingService {

    public CatsBookingServiceDtos.ResourceListResponse listResources(int page, int size, String category) {
        return toCats(super.listResourcesModel(page, size, category));
    }

    public CatsBookingServiceDtos.ResourceDetail getResource(String resourceId) {
        return toCats(super.getResourceModel(resourceId));
    }

    public CatsBookingServiceDtos.InventoryStatus getInventory(String resourceId, java.time.LocalDate date) {
        return toCats(super.getInventoryModel(resourceId, date));
    }

    public CatsBookingServiceDtos.ScheduleListResponse getSchedules(String resourceId,
                                                                    java.time.OffsetDateTime from,
                                                                    java.time.OffsetDateTime to) {
        return toCats(super.getSchedulesModel(resourceId, from, to));
    }

    public CatsBookingServiceDtos.DailyScheduleListResponse listDailySchedules(java.time.LocalDate date) {
        return toCats(super.listDailySchedulesModel(date));
    }

    public CatsBookingServiceDtos.ReservationCreateResponse createReservation(CatsBookingServiceDtos.ReservationCreateRequest request) {
        return toCats(super.createReservationModel(toDomain(request)));
    }

    public CatsBookingServiceDtos.ReservationCancelResponse cancelReservation(String reservationId,
                                                                              CatsBookingServiceDtos.ReservationCancelRequest request) {
        return toCats(super.cancelReservationModel(reservationId, toDomain(request)));
    }

    public CatsBookingServiceDtos.ReservationDetailResponse getReservation(String reservationId) {
        return toCats(super.getReservationModel(reservationId));
    }

    private BookingDomainModels.ReservationCreateRequest toDomain(CatsBookingServiceDtos.ReservationCreateRequest request) {
        return new BookingDomainModels.ReservationCreateRequest(
                request.resourceId(),
                request.scheduleId(),
                request.userId(),
                request.quantity(),
                request.memo()
        );
    }

    private BookingDomainModels.ReservationCancelRequest toDomain(CatsBookingServiceDtos.ReservationCancelRequest request) {
        return new BookingDomainModels.ReservationCancelRequest(request.reason());
    }

    private CatsBookingServiceDtos.ResourceListResponse toCats(BookingDomainModels.ResourceListResponse response) {
        return new CatsBookingServiceDtos.ResourceListResponse(
                response.items().stream().map(this::toCats).toList(),
                response.page(),
                response.size(),
                response.total()
        );
    }

    private CatsBookingServiceDtos.ResourceSummary toCats(BookingDomainModels.ResourceSummary summary) {
        return new CatsBookingServiceDtos.ResourceSummary(summary.resourceId(), summary.name(), summary.category(), summary.active());
    }

    private CatsBookingServiceDtos.ResourceDetail toCats(BookingDomainModels.ResourceDetail detail) {
        return new CatsBookingServiceDtos.ResourceDetail(
                detail.resourceId(),
                detail.name(),
                detail.category(),
                detail.active(),
                detail.description(),
                detail.location(),
                detail.timezone()
        );
    }

    private CatsBookingServiceDtos.InventoryStatus toCats(BookingDomainModels.InventoryStatus status) {
        return new CatsBookingServiceDtos.InventoryStatus(
                status.resourceId(),
                status.date(),
                status.totalQuantity(),
                status.availableQuantity(),
                status.reservedQuantity()
        );
    }

    private CatsBookingServiceDtos.ScheduleListResponse toCats(BookingDomainModels.ScheduleListResponse response) {
        return new CatsBookingServiceDtos.ScheduleListResponse(
                response.resourceId(),
                response.items().stream().map(this::toCats).toList()
        );
    }

    private CatsBookingServiceDtos.ScheduleItem toCats(BookingDomainModels.ScheduleItem item) {
        return new CatsBookingServiceDtos.ScheduleItem(item.scheduleId(), item.startAt(), item.endAt(), item.status());
    }

    private CatsBookingServiceDtos.DailyScheduleListResponse toCats(BookingDomainModels.DailyScheduleListResponse response) {
        return new CatsBookingServiceDtos.DailyScheduleListResponse(
                response.date(),
                response.items().stream().map(this::toCats).toList()
        );
    }

    private CatsBookingServiceDtos.DailyScheduleItem toCats(BookingDomainModels.DailyScheduleItem item) {
        return new CatsBookingServiceDtos.DailyScheduleItem(
                item.resourceId(),
                item.scheduleId(),
                item.startAt(),
                item.endAt(),
                item.status()
        );
    }

    private CatsBookingServiceDtos.ReservationCreateResponse toCats(BookingDomainModels.ReservationCreateResponse response) {
        return new CatsBookingServiceDtos.ReservationCreateResponse(response.reservationId(), response.status(), response.createdAt());
    }

    private CatsBookingServiceDtos.ReservationCancelResponse toCats(BookingDomainModels.ReservationCancelResponse response) {
        return new CatsBookingServiceDtos.ReservationCancelResponse(response.reservationId(), response.status(), response.canceledAt());
    }

    private CatsBookingServiceDtos.ReservationDetailResponse toCats(BookingDomainModels.ReservationDetailResponse response) {
        return new CatsBookingServiceDtos.ReservationDetailResponse(
                response.reservationId(),
                response.resourceId(),
                response.scheduleId(),
                response.userId(),
                response.quantity(),
                response.status(),
                response.createdAt(),
                response.canceledAt()
        );
    }
}
