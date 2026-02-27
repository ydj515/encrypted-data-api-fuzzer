package com.example.mockserver.controller;

import com.example.mockserver.dto.ApiDtos;
import com.example.mockserver.service.MockDomainService;
import com.example.mockserver.service.PayloadCryptoAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/cats/{org}/{service}")
public class MockApiController {

    private final MockDomainService domainService;
    private final PayloadCryptoAdapter payloadCryptoAdapter;
    private final ObjectMapper objectMapper;

    public MockApiController(MockDomainService domainService,
                             PayloadCryptoAdapter payloadCryptoAdapter,
                             ObjectMapper objectMapper) {
        this.domainService = domainService;
        this.payloadCryptoAdapter = payloadCryptoAdapter;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/resources")
    public ApiDtos.ResourceListResponse listResources(@PathVariable String org,
                                                      @PathVariable String service,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String category) {
        validateContext(org, service);
        return domainService.listResources(page, size, category);
    }

    @PostMapping("/resources")
    public Object listResourcesViaGateway(@PathVariable String org,
                                          @PathVariable String service,
                                          @RequestBody JsonNode body) {
        validateContext(org, service);

        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            int page = intValue(plain, "page", 0);
            int size = intValue(plain, "size", 20);
            String category = textValue(plain, "category", null);
            ApiDtos.ResourceListResponse response = domainService.listResources(page, size, category);
            return payloadCryptoAdapter.encryptResponse(response);
        }

        int page = intValue(body, "page", 0);
        int size = intValue(body, "size", 20);
        String category = textValue(body, "category", null);
        return domainService.listResources(page, size, category);
    }

    @GetMapping("/resources/{resourceId}")
    public ApiDtos.ResourceDetail getResource(@PathVariable String org,
                                              @PathVariable String service,
                                              @PathVariable String resourceId) {
        validateContext(org, service);
        return domainService.getResource(resourceId);
    }

    @PostMapping("/resources/{resourceId}")
    public Object getResourceViaGateway(@PathVariable String org,
                                        @PathVariable String service,
                                        @PathVariable String resourceId,
                                        @RequestBody(required = false) JsonNode body) {
        validateContext(org, service);
        ApiDtos.ResourceDetail response = domainService.getResource(resourceId);
        if (isEncryptedEnvelope(body)) {
            return payloadCryptoAdapter.encryptResponse(response);
        }
        return response;
    }

    @GetMapping("/resources/{resourceId}/inventory")
    public ApiDtos.InventoryStatus getInventory(@PathVariable String org,
                                                @PathVariable String service,
                                                @PathVariable String resourceId,
                                                @RequestParam LocalDate date) {
        validateContext(org, service);
        return domainService.getInventory(resourceId, date);
    }

    @PostMapping("/resources/{resourceId}/inventory")
    public Object getInventoryViaGateway(@PathVariable String org,
                                         @PathVariable String service,
                                         @PathVariable String resourceId,
                                         @RequestBody JsonNode body) {
        validateContext(org, service);

        LocalDate date;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            date = LocalDate.parse(requiredText(plain, "date"));
            return payloadCryptoAdapter.encryptResponse(domainService.getInventory(resourceId, date));
        }

        date = LocalDate.parse(requiredText(body, "date"));
        return domainService.getInventory(resourceId, date);
    }

    @GetMapping("/resources/{resourceId}/schedules")
    public ApiDtos.ScheduleListResponse getSchedules(@PathVariable String org,
                                                     @PathVariable String service,
                                                     @PathVariable String resourceId,
                                                     @RequestParam OffsetDateTime from,
                                                     @RequestParam OffsetDateTime to) {
        validateContext(org, service);
        return domainService.getSchedules(resourceId, from, to);
    }

    @PostMapping("/resources/{resourceId}/schedules")
    public Object getSchedulesViaGateway(@PathVariable String org,
                                         @PathVariable String service,
                                         @PathVariable String resourceId,
                                         @RequestBody JsonNode body) {
        validateContext(org, service);

        OffsetDateTime from;
        OffsetDateTime to;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            from = OffsetDateTime.parse(requiredText(plain, "from"));
            to = OffsetDateTime.parse(requiredText(plain, "to"));
            return payloadCryptoAdapter.encryptResponse(domainService.getSchedules(resourceId, from, to));
        }

        from = OffsetDateTime.parse(requiredText(body, "from"));
        to = OffsetDateTime.parse(requiredText(body, "to"));
        return domainService.getSchedules(resourceId, from, to);
    }

    @GetMapping("/schedules/daily")
    public ApiDtos.DailyScheduleListResponse listDailySchedules(@PathVariable String org,
                                                                @PathVariable String service,
                                                                @RequestParam LocalDate date) {
        validateContext(org, service);
        return domainService.listDailySchedules(date);
    }

    @PostMapping("/schedules/daily")
    public Object listDailySchedulesViaGateway(@PathVariable String org,
                                               @PathVariable String service,
                                               @RequestBody JsonNode body) {
        validateContext(org, service);

        LocalDate date;
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            date = LocalDate.parse(requiredText(plain, "date"));
            return payloadCryptoAdapter.encryptResponse(domainService.listDailySchedules(date));
        }

        date = LocalDate.parse(requiredText(body, "date"));
        return domainService.listDailySchedules(date);
    }

    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(@PathVariable String org,
                                               @PathVariable String service,
                                               @RequestBody JsonNode body) {
        validateContext(org, service);

        if (isEncryptedEnvelope(body)) {
            ApiDtos.ReservationCreateRequest request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), ApiDtos.ReservationCreateRequest.class);
            ApiDtos.ReservationCreateResponse response = domainService.createReservation(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(payloadCryptoAdapter.encryptResponse(response));
        }

        ApiDtos.ReservationCreateRequest request = objectMapper.convertValue(body, ApiDtos.ReservationCreateRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(domainService.createReservation(request));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public Object cancelReservation(@PathVariable String org,
                                    @PathVariable String service,
                                    @PathVariable String reservationId,
                                    @RequestBody JsonNode body) {
        validateContext(org, service);

        if (isEncryptedEnvelope(body)) {
            ApiDtos.ReservationCancelRequest request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), ApiDtos.ReservationCancelRequest.class);
            ApiDtos.ReservationCancelResponse response = domainService.cancelReservation(reservationId, request);
            return payloadCryptoAdapter.encryptResponse(response);
        }

        ApiDtos.ReservationCancelRequest request = objectMapper.convertValue(body, ApiDtos.ReservationCancelRequest.class);
        return domainService.cancelReservation(reservationId, request);
    }

    @GetMapping("/reservations/{reservationId}")
    public ApiDtos.ReservationDetailResponse getReservation(@PathVariable String org,
                                                            @PathVariable String service,
                                                            @PathVariable String reservationId) {
        validateContext(org, service);
        return domainService.getReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}")
    public Object getReservationViaGateway(@PathVariable String org,
                                           @PathVariable String service,
                                           @PathVariable String reservationId,
                                           @RequestBody(required = false) JsonNode body) {
        validateContext(org, service);
        ApiDtos.ReservationDetailResponse response = domainService.getReservation(reservationId);
        if (isEncryptedEnvelope(body)) {
            return payloadCryptoAdapter.encryptResponse(response);
        }
        return response;
    }

    private void validateContext(String org, String service) {
        if (org == null || org.isBlank() || service == null || service.isBlank()) {
            throw new IllegalArgumentException("org and service are required");
        }
    }

    private boolean isEncryptedEnvelope(JsonNode body) {
        return body != null && body.hasNonNull("data") && body.get("data").isTextual();
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = textValue(node, fieldName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String textValue(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asText();
    }

    private int intValue(JsonNode node, String fieldName, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asInt(defaultValue);
    }
}
