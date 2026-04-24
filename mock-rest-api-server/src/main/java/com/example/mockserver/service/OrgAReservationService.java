package com.example.mockserver.service;

import com.example.mockserver.dto.BookingDomainModels;
import com.example.mockserver.dto.OrgAReservationServiceDtos;
import org.springframework.stereotype.Service;

@Service
public class OrgAReservationService extends AbstractBookingService {

    public OrgAReservationServiceDtos.ResourceListResponse listResources(int page, int size, String category) {
        return toOrgA(super.listResourcesModel(page, size, category));
    }

    public OrgAReservationServiceDtos.ResourceDetail getResource(String resourceId) {
        return toOrgA(super.getResourceModel(resourceId));
    }

    public OrgAReservationServiceDtos.InventoryStatus getInventory(String resourceId, java.time.LocalDate date) {
        return toOrgA(super.getInventoryModel(resourceId, date));
    }

    public OrgAReservationServiceDtos.ScheduleListResponse getSchedules(String resourceId,
                                                                     java.time.OffsetDateTime from,
                                                                     java.time.OffsetDateTime to) {
        return toOrgA(super.getSchedulesModel(resourceId, from, to));
    }

    public OrgAReservationServiceDtos.DailyScheduleListResponse listDailySchedules(java.time.LocalDate date) {
        return toOrgA(super.listDailySchedulesModel(date));
    }

    public OrgAReservationServiceDtos.ReservationCreateResponse createReservation(OrgAReservationServiceDtos.ReservationCreateRequest request) {
        return toOrgA(super.createReservationModel(toDomain(request)));
    }

    public OrgAReservationServiceDtos.ReservationCancelResponse cancelReservation(String reservationId,
                                                                               OrgAReservationServiceDtos.ReservationCancelRequest request) {
        return toOrgA(super.cancelReservationModel(reservationId, toDomain(request)));
    }

    public OrgAReservationServiceDtos.ReservationDetailResponse getReservation(String reservationId) {
        return toOrgA(super.getReservationModel(reservationId));
    }

    private BookingDomainModels.ReservationCreateRequest toDomain(OrgAReservationServiceDtos.ReservationCreateRequest request) {
        return new BookingDomainModels.ReservationCreateRequest(
                request.resourceId(),
                request.scheduleId(),
                request.userId(),
                request.quantity(),
                request.memo()
        );
    }

    private BookingDomainModels.ReservationCancelRequest toDomain(OrgAReservationServiceDtos.ReservationCancelRequest request) {
        return new BookingDomainModels.ReservationCancelRequest(request.reason());
    }

    private OrgAReservationServiceDtos.ResourceListResponse toOrgA(BookingDomainModels.ResourceListResponse response) {
        return new OrgAReservationServiceDtos.ResourceListResponse(
                response.items().stream().map(this::toOrgA).toList(),
                response.page(),
                response.size(),
                response.total()
        );
    }

    private OrgAReservationServiceDtos.ResourceSummary toOrgA(BookingDomainModels.ResourceSummary summary) {
        return new OrgAReservationServiceDtos.ResourceSummary(summary.resourceId(), summary.name(), summary.category(), summary.active());
    }

    private OrgAReservationServiceDtos.ResourceDetail toOrgA(BookingDomainModels.ResourceDetail detail) {
        return new OrgAReservationServiceDtos.ResourceDetail(
                detail.resourceId(),
                detail.name(),
                detail.category(),
                detail.active(),
                detail.description(),
                detail.location(),
                detail.timezone()
        );
    }

    private OrgAReservationServiceDtos.InventoryStatus toOrgA(BookingDomainModels.InventoryStatus status) {
        return new OrgAReservationServiceDtos.InventoryStatus(
                status.resourceId(),
                status.date(),
                status.totalQuantity(),
                status.availableQuantity(),
                status.reservedQuantity()
        );
    }

    private OrgAReservationServiceDtos.ScheduleListResponse toOrgA(BookingDomainModels.ScheduleListResponse response) {
        return new OrgAReservationServiceDtos.ScheduleListResponse(
                response.resourceId(),
                response.items().stream().map(this::toOrgA).toList()
        );
    }

    private OrgAReservationServiceDtos.ScheduleItem toOrgA(BookingDomainModels.ScheduleItem item) {
        return new OrgAReservationServiceDtos.ScheduleItem(item.scheduleId(), item.startAt(), item.endAt(), item.status());
    }

    private OrgAReservationServiceDtos.DailyScheduleListResponse toOrgA(BookingDomainModels.DailyScheduleListResponse response) {
        return new OrgAReservationServiceDtos.DailyScheduleListResponse(
                response.date(),
                response.items().stream().map(this::toOrgA).toList()
        );
    }

    private OrgAReservationServiceDtos.DailyScheduleItem toOrgA(BookingDomainModels.DailyScheduleItem item) {
        return new OrgAReservationServiceDtos.DailyScheduleItem(
                item.resourceId(),
                item.scheduleId(),
                item.startAt(),
                item.endAt(),
                item.status()
        );
    }

    private OrgAReservationServiceDtos.ReservationCreateResponse toOrgA(BookingDomainModels.ReservationCreateResponse response) {
        return new OrgAReservationServiceDtos.ReservationCreateResponse(response.reservationId(), response.status(), response.createdAt());
    }

    private OrgAReservationServiceDtos.ReservationCancelResponse toOrgA(BookingDomainModels.ReservationCancelResponse response) {
        return new OrgAReservationServiceDtos.ReservationCancelResponse(response.reservationId(), response.status(), response.canceledAt());
    }

    private OrgAReservationServiceDtos.ReservationDetailResponse toOrgA(BookingDomainModels.ReservationDetailResponse response) {
        return new OrgAReservationServiceDtos.ReservationDetailResponse(
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
