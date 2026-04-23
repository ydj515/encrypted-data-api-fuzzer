package com.example.mockserver.controller;

import com.example.mockserver.dto.OrgAServiceAServiceDtos;
import com.example.mockserver.service.OrgAServiceAService;
import com.example.mockserver.service.PayloadCryptoAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cats/orgA/A")
public class OrgAServiceAServiceController extends AbstractBookingServiceController {

    private final OrgAServiceAService orgAServiceAService;

    public OrgAServiceAServiceController(OrgAServiceAService orgAServiceAService,
                                         PayloadCryptoAdapter payloadCryptoAdapter,
                                         ObjectMapper objectMapper) {
        super(payloadCryptoAdapter, objectMapper);
        this.orgAServiceAService = orgAServiceAService;
    }

    @Override
    protected Object listResourcesInternal(int page, int size, String category) {
        return orgAServiceAService.listResources(page, size, category);
    }

    @Override
    protected Object getResourceInternal(String resourceId) {
        return orgAServiceAService.getResource(resourceId);
    }

    @Override
    protected Object getInventoryInternal(String resourceId, java.time.LocalDate date) {
        return orgAServiceAService.getInventory(resourceId, date);
    }

    @Override
    protected Object getSchedulesInternal(String resourceId, java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        return orgAServiceAService.getSchedules(resourceId, from, to);
    }

    @Override
    protected Object listDailySchedulesInternal(java.time.LocalDate date) {
        return orgAServiceAService.listDailySchedules(date);
    }

    @Override
    protected Class<?> reservationCreateRequestType() {
        return OrgAServiceAServiceDtos.ReservationCreateRequest.class;
    }

    @Override
    protected Object createReservationInternal(Object request) {
        return orgAServiceAService.createReservation((OrgAServiceAServiceDtos.ReservationCreateRequest) request);
    }

    @Override
    protected Class<?> reservationCancelRequestType() {
        return OrgAServiceAServiceDtos.ReservationCancelRequest.class;
    }

    @Override
    protected Object cancelReservationInternal(String reservationId, Object request) {
        return orgAServiceAService.cancelReservation(reservationId, (OrgAServiceAServiceDtos.ReservationCancelRequest) request);
    }

    @Override
    protected Object getReservationInternal(String reservationId) {
        return orgAServiceAService.getReservation(reservationId);
    }
}
