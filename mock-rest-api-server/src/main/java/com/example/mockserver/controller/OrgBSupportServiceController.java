package com.example.mockserver.controller;

import com.example.mockserver.dto.OrgBSupportServiceDtos;
import com.example.mockserver.service.OrgBSupportService;
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

@RestController
@RequestMapping("/cats/orgB/support")
public class OrgBSupportServiceController extends AbstractMockApiController {

    private final OrgBSupportService supportService;

    public OrgBSupportServiceController(OrgBSupportService supportService,
                                        PayloadCryptoAdapter payloadCryptoAdapter,
                                        ObjectMapper objectMapper) {
        super(payloadCryptoAdapter, objectMapper);
        this.supportService = supportService;
    }

    @GetMapping("/devices")
    public OrgBSupportServiceDtos.DeviceListResponse listDevices(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam(required = false) String status) {
        return supportService.listDevices(page, size, status);
    }

    @PostMapping("/devices")
    public Object listDevicesViaGateway(@RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            JsonNode plain = payloadCryptoAdapter.decryptToJson(body.get("data").asText());
            OrgBSupportServiceDtos.DeviceListResponse response = supportService.listDevices(
                    intValue(plain, "page", 0),
                    intValue(plain, "size", 20),
                    textValue(plain, "status", null)
            );
            return payloadCryptoAdapter.encryptResponse(response);
        }

        return supportService.listDevices(
                intValue(body, "page", 0),
                intValue(body, "size", 20),
                textValue(body, "status", null)
        );
    }

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(@RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            OrgBSupportServiceDtos.SupportTicketCreateRequest request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), OrgBSupportServiceDtos.SupportTicketCreateRequest.class);
            OrgBSupportServiceDtos.SupportTicketCreateResponse response = supportService.createTicket(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(payloadCryptoAdapter.encryptResponse(response));
        }

        OrgBSupportServiceDtos.SupportTicketCreateRequest request = objectMapper.convertValue(body, OrgBSupportServiceDtos.SupportTicketCreateRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.createTicket(request));
    }

    @GetMapping("/tickets/{ticketId}")
    public OrgBSupportServiceDtos.SupportTicketDetailResponse getTicket(@PathVariable String ticketId) {
        return supportService.getTicket(ticketId);
    }

    @PostMapping("/tickets/{ticketId}")
    public Object getTicketViaGateway(@PathVariable String ticketId,
                                      @RequestBody(required = false) JsonNode body) {
        OrgBSupportServiceDtos.SupportTicketDetailResponse response = supportService.getTicket(ticketId);
        if (isEncryptedEnvelope(body)) {
            return payloadCryptoAdapter.encryptResponse(response);
        }
        return response;
    }

    @PostMapping("/tickets/{ticketId}/resolve")
    public Object resolveTicket(@PathVariable String ticketId,
                                @RequestBody JsonNode body) {
        if (isEncryptedEnvelope(body)) {
            OrgBSupportServiceDtos.SupportResolveRequest request = payloadCryptoAdapter
                    .decryptToObject(body.get("data").asText(), OrgBSupportServiceDtos.SupportResolveRequest.class);
            OrgBSupportServiceDtos.SupportResolveResponse response = supportService.resolveTicket(ticketId, request);
            return payloadCryptoAdapter.encryptResponse(response);
        }

        OrgBSupportServiceDtos.SupportResolveRequest request = objectMapper.convertValue(body, OrgBSupportServiceDtos.SupportResolveRequest.class);
        return supportService.resolveTicket(ticketId, request);
    }
}
