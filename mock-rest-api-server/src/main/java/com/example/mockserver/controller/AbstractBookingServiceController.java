package com.example.mockserver.controller;

import com.example.mockserver.service.PayloadCryptoAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public abstract class AbstractBookingServiceController extends AbstractMockApiController {

    protected AbstractBookingServiceController(PayloadCryptoAdapter payloadCryptoAdapter,
                                               ObjectMapper objectMapper) {
        super(payloadCryptoAdapter, objectMapper);
    }

    @GetMapping("/resources")
    public Object listResources(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String category) {
        return listResourcesInternal(page, size, category);
    }

    @PostMapping("/resources")
    public Object listResourcesViaGateway(@RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            int page = intValue(plain, "page", 0);
            int size = intValue(plain, "size", 20);
            String category = textValue(plain, "category", null);
            Object response = listResourcesInternal(page, size, category);
            return payloadCryptoAdapter.encryptResponse(response);
        }

        int page = intValue(body, "page", 0);
        int size = intValue(body, "size", 20);
        String category = textValue(body, "category", null);
        return listResourcesInternal(page, size, category);
    }

    @GetMapping("/resources/{resourceId}")
    public Object getResource(@PathVariable String resourceId) {
        return getResourceInternal(resourceId);
    }

    @PostMapping("/resources/{resourceId}")
    public Object getResourceViaGateway(@PathVariable String resourceId,
                                        @RequestBody(required = false) JsonNode body) {
        Object response = getResourceInternal(resourceId);
        if (isEncryptedEnvelope(body)) {
            return payloadCryptoAdapter.encryptResponse(response);
        }
        return response;
    }

    @GetMapping("/resources/{resourceId}/inventory")
    public Object getInventory(@PathVariable String resourceId,
                               @RequestParam LocalDate date) {
        return getInventoryInternal(resourceId, date);
    }

    @PostMapping("/resources/{resourceId}/inventory")
    public Object getInventoryViaGateway(@PathVariable String resourceId,
                                         @RequestBody JsonNode body) {
        LocalDate date;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            date = LocalDate.parse(requiredText(plain, "date"));
            return payloadCryptoAdapter.encryptResponse(getInventoryInternal(resourceId, date));
        }

        date = LocalDate.parse(requiredText(body, "date"));
        return getInventoryInternal(resourceId, date);
    }

    @GetMapping("/resources/{resourceId}/schedules")
    public Object getSchedules(@PathVariable String resourceId,
                               @RequestParam OffsetDateTime from,
                               @RequestParam OffsetDateTime to) {
        return getSchedulesInternal(resourceId, from, to);
    }

    @PostMapping("/resources/{resourceId}/schedules")
    public Object getSchedulesViaGateway(@PathVariable String resourceId,
                                         @RequestBody JsonNode body) {
        OffsetDateTime from;
        OffsetDateTime to;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            from = OffsetDateTime.parse(requiredText(plain, "from"));
            to = OffsetDateTime.parse(requiredText(plain, "to"));
            return payloadCryptoAdapter.encryptResponse(getSchedulesInternal(resourceId, from, to));
        }

        from = OffsetDateTime.parse(requiredText(body, "from"));
        to = OffsetDateTime.parse(requiredText(body, "to"));
        return getSchedulesInternal(resourceId, from, to);
    }

    @GetMapping("/schedules/daily")
    public Object listDailySchedules(@RequestParam LocalDate date) {
        return listDailySchedulesInternal(date);
    }

    @PostMapping("/schedules/daily")
    public Object listDailySchedulesViaGateway(@RequestBody JsonNode body) {
        LocalDate date;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            date = LocalDate.parse(requiredText(plain, "date"));
            return payloadCryptoAdapter.encryptResponse(listDailySchedulesInternal(date));
        }

        date = LocalDate.parse(requiredText(body, "date"));
        return listDailySchedulesInternal(date);
    }

    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(@RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            Object request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), reservationCreateRequestType());
            Object response = createReservationInternal(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(payloadCryptoAdapter.encryptResponse(response));
        }

        Object request = objectMapper.convertValue(body, reservationCreateRequestType());
        return ResponseEntity.status(HttpStatus.CREATED).body(createReservationInternal(request));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public Object cancelReservation(@PathVariable String reservationId,
                                    @RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            Object request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), reservationCancelRequestType());
            Object response = cancelReservationInternal(reservationId, request);
            return payloadCryptoAdapter.encryptResponse(response);
        }

        Object request = objectMapper.convertValue(body, reservationCancelRequestType());
        return cancelReservationInternal(reservationId, request);
    }

    @GetMapping("/reservations/{reservationId}")
    public Object getReservation(@PathVariable String reservationId) {
        return getReservationInternal(reservationId);
    }

    @PostMapping("/reservations/{reservationId}")
    public Object getReservationViaGateway(@PathVariable String reservationId,
                                           @RequestBody(required = false) JsonNode body) {
        Object response = getReservationInternal(reservationId);
        if (isEncryptedEnvelope(body)) {
            return payloadCryptoAdapter.encryptResponse(response);
        }
        return response;
    }

    protected abstract Object listResourcesInternal(int page, int size, String category);

    protected abstract Object getResourceInternal(String resourceId);

    protected abstract Object getInventoryInternal(String resourceId, LocalDate date);

    protected abstract Object getSchedulesInternal(String resourceId, OffsetDateTime from, OffsetDateTime to);

    protected abstract Object listDailySchedulesInternal(LocalDate date);

    protected abstract Class<?> reservationCreateRequestType();

    protected abstract Object createReservationInternal(Object request);

    protected abstract Class<?> reservationCancelRequestType();

    protected abstract Object cancelReservationInternal(String reservationId, Object request);

    protected abstract Object getReservationInternal(String reservationId);
}
