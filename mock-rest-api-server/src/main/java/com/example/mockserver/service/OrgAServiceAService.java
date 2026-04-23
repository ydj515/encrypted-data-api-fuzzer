package com.example.mockserver.service;

import com.example.mockserver.dto.BookingDomainModels;
import com.example.mockserver.dto.OrgAServiceAServiceDtos;
import org.springframework.stereotype.Service;

@Service
public class OrgAServiceAService extends AbstractBookingService {

    public OrgAServiceAServiceDtos.ResourceListResponse listResources(int page, int size, String category) {
        return toOrgA(super.listResourcesModel(page, size, category));
    }

    public OrgAServiceAServiceDtos.ResourceDetail getResource(String resourceId) {
        return toOrgA(super.getResourceModel(resourceId));
    }

    public OrgAServiceAServiceDtos.InventoryStatus getInventory(String resourceId, java.time.LocalDate date) {
        return toOrgA(super.getInventoryModel(resourceId, date));
    }

    public OrgAServiceAServiceDtos.ScheduleListResponse getSchedules(String resourceId,
                                                                     java.time.OffsetDateTime from,
                                                                     java.time.OffsetDateTime to) {
        return toOrgA(super.getSchedulesModel(resourceId, from, to));
    }

    public OrgAServiceAServiceDtos.DailyScheduleListResponse listDailySchedules(java.time.LocalDate date) {
        return toOrgA(super.listDailySchedulesModel(date));
    }

    public OrgAServiceAServiceDtos.ReservationCreateResponse createReservation(OrgAServiceAServiceDtos.ReservationCreateRequest request) {
        return toOrgA(super.createReservationModel(toDomain(request)));
    }

    public OrgAServiceAServiceDtos.ReservationCancelResponse cancelReservation(String reservationId,
                                                                               OrgAServiceAServiceDtos.ReservationCancelRequest request) {
        return toOrgA(super.cancelReservationModel(reservationId, toDomain(request)));
    }

    public OrgAServiceAServiceDtos.ReservationDetailResponse getReservation(String reservationId) {
        return toOrgA(super.getReservationModel(reservationId));
    }

    private BookingDomainModels.ReservationCreateRequest toDomain(OrgAServiceAServiceDtos.ReservationCreateRequest request) {
        return new BookingDomainModels.ReservationCreateRequest(
                request.resourceId(),
                request.scheduleId(),
                request.userId(),
                request.quantity(),
                request.memo()
        );
    }

    private BookingDomainModels.ReservationCancelRequest toDomain(OrgAServiceAServiceDtos.ReservationCancelRequest request) {
        return new BookingDomainModels.ReservationCancelRequest(request.reason());
    }

    private OrgAServiceAServiceDtos.ResourceListResponse toOrgA(BookingDomainModels.ResourceListResponse response) {
        return new OrgAServiceAServiceDtos.ResourceListResponse(
                response.items().stream().map(this::toOrgA).toList(),
                response.page(),
                response.size(),
                response.total()
        );
    }

    private OrgAServiceAServiceDtos.ResourceSummary toOrgA(BookingDomainModels.ResourceSummary summary) {
        return new OrgAServiceAServiceDtos.ResourceSummary(summary.resourceId(), summary.name(), summary.category(), summary.active());
    }

    private OrgAServiceAServiceDtos.ResourceDetail toOrgA(BookingDomainModels.ResourceDetail detail) {
        return new OrgAServiceAServiceDtos.ResourceDetail(
                detail.resourceId(),
                detail.name(),
                detail.category(),
                detail.active(),
                detail.description(),
                detail.location(),
                detail.timezone()
        );
    }

    private OrgAServiceAServiceDtos.InventoryStatus toOrgA(BookingDomainModels.InventoryStatus status) {
        return new OrgAServiceAServiceDtos.InventoryStatus(
                status.resourceId(),
                status.date(),
                status.totalQuantity(),
                status.availableQuantity(),
                status.reservedQuantity()
        );
    }

    private OrgAServiceAServiceDtos.ScheduleListResponse toOrgA(BookingDomainModels.ScheduleListResponse response) {
        return new OrgAServiceAServiceDtos.ScheduleListResponse(
                response.resourceId(),
                response.items().stream().map(this::toOrgA).toList()
        );
    }

    private OrgAServiceAServiceDtos.ScheduleItem toOrgA(BookingDomainModels.ScheduleItem item) {
        return new OrgAServiceAServiceDtos.ScheduleItem(item.scheduleId(), item.startAt(), item.endAt(), item.status());
    }

    private OrgAServiceAServiceDtos.DailyScheduleListResponse toOrgA(BookingDomainModels.DailyScheduleListResponse response) {
        return new OrgAServiceAServiceDtos.DailyScheduleListResponse(
                response.date(),
                response.items().stream().map(this::toOrgA).toList()
        );
    }

    private OrgAServiceAServiceDtos.DailyScheduleItem toOrgA(BookingDomainModels.DailyScheduleItem item) {
        return new OrgAServiceAServiceDtos.DailyScheduleItem(
                item.resourceId(),
                item.scheduleId(),
                item.startAt(),
                item.endAt(),
                item.status()
        );
    }

    private OrgAServiceAServiceDtos.ReservationCreateResponse toOrgA(BookingDomainModels.ReservationCreateResponse response) {
        return new OrgAServiceAServiceDtos.ReservationCreateResponse(response.reservationId(), response.status(), response.createdAt());
    }

    private OrgAServiceAServiceDtos.ReservationCancelResponse toOrgA(BookingDomainModels.ReservationCancelResponse response) {
        return new OrgAServiceAServiceDtos.ReservationCancelResponse(response.reservationId(), response.status(), response.canceledAt());
    }

    private OrgAServiceAServiceDtos.ReservationDetailResponse toOrgA(BookingDomainModels.ReservationDetailResponse response) {
        return new OrgAServiceAServiceDtos.ReservationDetailResponse(
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
