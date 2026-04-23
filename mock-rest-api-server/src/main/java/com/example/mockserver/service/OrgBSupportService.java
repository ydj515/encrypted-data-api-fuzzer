package com.example.mockserver.service;

import com.example.mockserver.dto.OrgBSupportServiceDtos;
import com.example.mockserver.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrgBSupportService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE_NUMBER = 1_000;

    private final Map<String, OrgBSupportServiceDtos.DeviceSummary> devices = new ConcurrentHashMap<>();
    private final Map<String, SupportTicketEntity> tickets = new ConcurrentHashMap<>();

    public OrgBSupportService() {
        initializeDevices();
    }

    public OrgBSupportServiceDtos.DeviceListResponse listDevices(int page, int size, String status) {
        validatePageRequest(page, size);

        List<OrgBSupportServiceDtos.DeviceSummary> filtered = devices.values().stream()
                .filter(device -> status == null || status.isBlank() || device.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(OrgBSupportServiceDtos.DeviceSummary::deviceId))
                .toList();

        long offset = (long) page * size;
        int from = (int) Math.min(offset, filtered.size());
        int to = (int) Math.min(offset + size, filtered.size());
        return new OrgBSupportServiceDtos.DeviceListResponse(filtered.subList(from, to), page, size, filtered.size());
    }

    public OrgBSupportServiceDtos.SupportTicketCreateResponse createTicket(OrgBSupportServiceDtos.SupportTicketCreateRequest request) {
        validateCreateRequest(request);
        getDevice(request.deviceId());

        String ticketId = "TCK-" + System.currentTimeMillis();
        OffsetDateTime now = OffsetDateTime.now();
        tickets.put(ticketId, new SupportTicketEntity(
                ticketId,
                request.deviceId(),
                request.requesterId(),
                request.issueType(),
                request.description(),
                null,
                "OPEN",
                now,
                null
        ));

        return new OrgBSupportServiceDtos.SupportTicketCreateResponse(ticketId, "OPEN", now);
    }

    public OrgBSupportServiceDtos.SupportTicketDetailResponse getTicket(String ticketId) {
        SupportTicketEntity entity = getTicketEntity(ticketId);
        return new OrgBSupportServiceDtos.SupportTicketDetailResponse(
                entity.ticketId(),
                entity.deviceId(),
                entity.requesterId(),
                entity.issueType(),
                entity.description(),
                entity.resolutionNote(),
                entity.status(),
                entity.createdAt(),
                entity.resolvedAt()
        );
    }

    public OrgBSupportServiceDtos.SupportResolveResponse resolveTicket(String ticketId, OrgBSupportServiceDtos.SupportResolveRequest request) {
        if (request == null || isBlank(request.resolutionNote())) {
            throw new ApiException("INVALID_REQUEST", "resolutionNote is required", HttpStatus.BAD_REQUEST.value());
        }

        SupportTicketEntity current = getTicketEntity(ticketId);
        if ("RESOLVED".equals(current.status())) {
            return new OrgBSupportServiceDtos.SupportResolveResponse(ticketId, "RESOLVED", current.resolvedAt());
        }

        OffsetDateTime resolvedAt = OffsetDateTime.now();
        tickets.put(ticketId, current.resolve(request.resolutionNote(), resolvedAt));
        return new OrgBSupportServiceDtos.SupportResolveResponse(ticketId, "RESOLVED", resolvedAt);
    }

    private OrgBSupportServiceDtos.DeviceSummary getDevice(String deviceId) {
        OrgBSupportServiceDtos.DeviceSummary device = devices.get(deviceId);
        if (device == null) {
            throw new ApiException("DEVICE_NOT_FOUND", "Device not found: " + deviceId, HttpStatus.NOT_FOUND.value());
        }
        return device;
    }

    private SupportTicketEntity getTicketEntity(String ticketId) {
        SupportTicketEntity entity = tickets.get(ticketId);
        if (entity == null) {
            throw new ApiException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId, HttpStatus.NOT_FOUND.value());
        }
        return entity;
    }

    private void validateCreateRequest(OrgBSupportServiceDtos.SupportTicketCreateRequest request) {
        if (request == null) {
            throw new ApiException("INVALID_REQUEST", "Request body is required", HttpStatus.BAD_REQUEST.value());
        }
        if (isBlank(request.deviceId()) || isBlank(request.requesterId()) || isBlank(request.issueType())) {
            throw new ApiException("INVALID_REQUEST", "deviceId, requesterId, issueType are required", HttpStatus.BAD_REQUEST.value());
        }
        if (isBlank(request.description())) {
            throw new ApiException("INVALID_REQUEST", "description is required", HttpStatus.BAD_REQUEST.value());
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || page > MAX_PAGE_NUMBER) {
            throw new ApiException("INVALID_REQUEST", "page must be between 0 and " + MAX_PAGE_NUMBER, HttpStatus.BAD_REQUEST.value());
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException("INVALID_REQUEST", "size must be between 1 and " + MAX_PAGE_SIZE, HttpStatus.BAD_REQUEST.value());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void initializeDevices() {
        devices.put("DEV-01", new OrgBSupportServiceDtos.DeviceSummary("DEV-01", "Entrance Kiosk", "K-100", "ACTIVE"));
        devices.put("DEV-02", new OrgBSupportServiceDtos.DeviceSummary("DEV-02", "Badge Printer", "P-220", "ACTIVE"));
        devices.put("DEV-03", new OrgBSupportServiceDtos.DeviceSummary("DEV-03", "Visitor Tablet", "T-330", "MAINTENANCE"));
        devices.put("DEV-04", new OrgBSupportServiceDtos.DeviceSummary("DEV-04", "Lobby Display", "D-410", "ACTIVE"));
    }

    private record SupportTicketEntity(String ticketId,
                                       String deviceId,
                                       String requesterId,
                                       String issueType,
                                       String description,
                                       String resolutionNote,
                                       String status,
                                       OffsetDateTime createdAt,
                                       OffsetDateTime resolvedAt) {
        private SupportTicketEntity resolve(String nextResolutionNote, OffsetDateTime nextResolvedAt) {
            return new SupportTicketEntity(
                    ticketId,
                    deviceId,
                    requesterId,
                    issueType,
                    description,
                    nextResolutionNote,
                    "RESOLVED",
                    createdAt,
                    nextResolvedAt
            );
        }
    }
}
