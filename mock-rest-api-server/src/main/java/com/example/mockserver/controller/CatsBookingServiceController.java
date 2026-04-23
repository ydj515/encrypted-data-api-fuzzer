package com.example.mockserver.controller;

import com.example.mockserver.dto.CatsBookingServiceDtos;
import com.example.mockserver.service.CatsBookingService;
import com.example.mockserver.service.PayloadCryptoAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cats/catsOrg/booking")
public class CatsBookingServiceController extends AbstractBookingServiceController {

    private final CatsBookingService catsBookingService;

    public CatsBookingServiceController(CatsBookingService catsBookingService,
                                        PayloadCryptoAdapter payloadCryptoAdapter,
                                        ObjectMapper objectMapper) {
        super(payloadCryptoAdapter, objectMapper);
        this.catsBookingService = catsBookingService;
    }

    @Override
    protected Object listResourcesInternal(int page, int size, String category) {
        return catsBookingService.listResources(page, size, category);
    }

    @Override
    protected Object getResourceInternal(String resourceId) {
        return catsBookingService.getResource(resourceId);
    }

    @Override
    protected Object getInventoryInternal(String resourceId, java.time.LocalDate date) {
        return catsBookingService.getInventory(resourceId, date);
    }

    @Override
    protected Object getSchedulesInternal(String resourceId, java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        return catsBookingService.getSchedules(resourceId, from, to);
    }

    @Override
    protected Object listDailySchedulesInternal(java.time.LocalDate date) {
        return catsBookingService.listDailySchedules(date);
    }

    @Override
    protected Class<?> reservationCreateRequestType() {
        return CatsBookingServiceDtos.ReservationCreateRequest.class;
    }

    @Override
    protected Object createReservationInternal(Object request) {
        return catsBookingService.createReservation((CatsBookingServiceDtos.ReservationCreateRequest) request);
    }

    @Override
    protected Class<?> reservationCancelRequestType() {
        return CatsBookingServiceDtos.ReservationCancelRequest.class;
    }

    @Override
    protected Object cancelReservationInternal(String reservationId, Object request) {
        return catsBookingService.cancelReservation(reservationId, (CatsBookingServiceDtos.ReservationCancelRequest) request);
    }

    @Override
    protected Object getReservationInternal(String reservationId) {
        return catsBookingService.getReservation(reservationId);
    }
}
