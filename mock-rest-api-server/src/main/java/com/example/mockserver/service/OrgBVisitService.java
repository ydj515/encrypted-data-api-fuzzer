package com.example.mockserver.service;

import com.example.mockserver.dto.OrgBVisitServiceDtos;
import com.example.mockserver.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrgBVisitService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE_NUMBER = 1_000;
    private static final int TOTAL_DAILY_SLOTS = 20;

    private final Map<String, OrgBVisitServiceDtos.SiteSummary> sites = new ConcurrentHashMap<>();
    private final Map<String, VisitEntity> visits = new ConcurrentHashMap<>();

    public OrgBVisitService() {
        initializeSites();
    }

    public OrgBVisitServiceDtos.SiteListResponse listSites(int page, int size, String city) {
        validatePageRequest(page, size);

        List<OrgBVisitServiceDtos.SiteSummary> filtered = sites.values().stream()
                .filter(site -> city == null || city.isBlank() || site.city().equalsIgnoreCase(city))
                .sorted(Comparator.comparing(OrgBVisitServiceDtos.SiteSummary::siteId))
                .toList();

        long offset = (long) page * size;
        int from = (int) Math.min(offset, filtered.size());
        int to = (int) Math.min(offset + size, filtered.size());
        return new OrgBVisitServiceDtos.SiteListResponse(filtered.subList(from, to), page, size, filtered.size());
    }

    public OrgBVisitServiceDtos.VisitSlotStatus getSlotStatus(String siteId, LocalDate date) {
        getSite(siteId);

        int reserved = visits.values().stream()
                .filter(visit -> visit.siteId().equals(siteId))
                .filter(visit -> visit.visitDate().equals(date))
                .filter(visit -> !"CANCELED".equals(visit.status()))
                .mapToInt(VisitEntity::partySize)
                .sum();

        int available = Math.max(0, TOTAL_DAILY_SLOTS - reserved);
        return new OrgBVisitServiceDtos.VisitSlotStatus(siteId, date, TOTAL_DAILY_SLOTS, available, reserved);
    }

    public OrgBVisitServiceDtos.VisitCreateResponse createVisit(OrgBVisitServiceDtos.VisitCreateRequest request) {
        validateCreateRequest(request);
        getSite(request.siteId());

        int available = getSlotStatus(request.siteId(), request.visitDate()).availableSlots();
        if (request.partySize() > available) {
            throw new ApiException("VISIT_CONFLICT", "Insufficient visit slots", HttpStatus.CONFLICT.value());
        }

        String visitId = "VIS-" + System.currentTimeMillis();
        OffsetDateTime now = OffsetDateTime.now();
        visits.put(visitId, new VisitEntity(
                visitId,
                request.siteId(),
                request.visitDate(),
                request.visitorName(),
                request.visitorPhone(),
                request.partySize(),
                request.purpose(),
                "REQUESTED",
                now,
                null
        ));

        return new OrgBVisitServiceDtos.VisitCreateResponse(visitId, "REQUESTED", now);
    }

    public OrgBVisitServiceDtos.VisitDetailResponse getVisit(String visitId) {
        VisitEntity visit = getVisitEntity(visitId);
        return new OrgBVisitServiceDtos.VisitDetailResponse(
                visit.visitId(),
                visit.siteId(),
                visit.visitDate(),
                visit.visitorName(),
                visit.visitorPhone(),
                visit.partySize(),
                visit.purpose(),
                visit.status(),
                visit.createdAt(),
                visit.canceledAt()
        );
    }

    public OrgBVisitServiceDtos.VisitCancelResponse cancelVisit(String visitId, OrgBVisitServiceDtos.VisitCancelRequest request) {
        if (request == null || isBlank(request.reason())) {
            throw new ApiException("INVALID_REQUEST", "Cancel reason is required", HttpStatus.BAD_REQUEST.value());
        }

        VisitEntity current = getVisitEntity(visitId);
        if ("CANCELED".equals(current.status())) {
            return new OrgBVisitServiceDtos.VisitCancelResponse(visitId, "CANCELED", current.canceledAt());
        }

        OffsetDateTime canceledAt = OffsetDateTime.now();
        visits.put(visitId, current.withStatus("CANCELED", canceledAt));
        return new OrgBVisitServiceDtos.VisitCancelResponse(visitId, "CANCELED", canceledAt);
    }

    private OrgBVisitServiceDtos.SiteSummary getSite(String siteId) {
        OrgBVisitServiceDtos.SiteSummary site = sites.get(siteId);
        if (site == null) {
            throw new ApiException("SITE_NOT_FOUND", "Site not found: " + siteId, HttpStatus.NOT_FOUND.value());
        }
        return site;
    }

    private VisitEntity getVisitEntity(String visitId) {
        VisitEntity entity = visits.get(visitId);
        if (entity == null) {
            throw new ApiException("VISIT_NOT_FOUND", "Visit not found: " + visitId, HttpStatus.NOT_FOUND.value());
        }
        return entity;
    }

    private void validateCreateRequest(OrgBVisitServiceDtos.VisitCreateRequest request) {
        if (request == null) {
            throw new ApiException("INVALID_REQUEST", "Request body is required", HttpStatus.BAD_REQUEST.value());
        }
        if (isBlank(request.siteId()) || request.visitDate() == null || isBlank(request.visitorName())) {
            throw new ApiException("INVALID_REQUEST", "siteId, visitDate, visitorName are required", HttpStatus.BAD_REQUEST.value());
        }
        if (request.partySize() == null || request.partySize() < 1 || request.partySize() > 8) {
            throw new ApiException("INVALID_REQUEST", "partySize must be 1..8", HttpStatus.BAD_REQUEST.value());
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

    private void initializeSites() {
        sites.put("SITE-01", new OrgBVisitServiceDtos.SiteSummary("SITE-01", "Main Campus", "Seoul", true));
        sites.put("SITE-02", new OrgBVisitServiceDtos.SiteSummary("SITE-02", "Research Lab", "Seoul", true));
        sites.put("SITE-03", new OrgBVisitServiceDtos.SiteSummary("SITE-03", "Regional Center", "Busan", true));
        sites.put("SITE-04", new OrgBVisitServiceDtos.SiteSummary("SITE-04", "Archive Hall", "Daejeon", true));
    }

    private record VisitEntity(String visitId,
                               String siteId,
                               LocalDate visitDate,
                               String visitorName,
                               String visitorPhone,
                               int partySize,
                               String purpose,
                               String status,
                               OffsetDateTime createdAt,
                               OffsetDateTime canceledAt) {
        private VisitEntity withStatus(String nextStatus, OffsetDateTime nextCanceledAt) {
            return new VisitEntity(
                    visitId,
                    siteId,
                    visitDate,
                    visitorName,
                    visitorPhone,
                    partySize,
                    purpose,
                    nextStatus,
                    createdAt,
                    nextCanceledAt
            );
        }
    }
}
