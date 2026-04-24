package com.example.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MockRestApiServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void resourceListShouldReturnAtLeastTenItems() throws Exception {
        mockMvc.perform(get("/cats/catsOrg/booking/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12));
    }

    @Test
    void dailyScheduleListShouldReturnAtLeastTenItems() throws Exception {
        mockMvc.perform(get("/cats/catsOrg/booking/schedules/daily")
                        .queryParam("date", "2026-02-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12));
    }

    @Test
    void encryptedInventoryRequestShouldReturnEncryptedDataEnvelope() throws Exception {
        String plain = objectMapper.writeValueAsString(Map.of("date", "2026-02-26"));
        String encrypted = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/cats/catsOrg/booking/resources/R-001/inventory")
                        .contentType("application/json")
                        .content("{\"data\":\"" + encrypted + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void encryptedCreateReservationShouldReturnEncryptedDataEnvelope() throws Exception {
        String plain = objectMapper.writeValueAsString(Map.of(
                "resourceId", "R-001",
                "scheduleId", "R-001-2026-02-26-9",
                "userId", "user-001",
                "quantity", 1,
                "memo", "encrypted create"
        ));
        String encrypted = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/cats/catsOrg/booking/reservations")
                        .contentType("application/json")
                        .content("{\"data\":\"" + encrypted + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void invalidQueryDateShouldReturn400() throws Exception {
        mockMvc.perform(get("/cats/catsOrg/booking/schedules/daily")
                        .queryParam("date", "2026-ABUGIDA-26"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void invalidEncryptedDateTimeShouldReturn400() throws Exception {
        String plain = objectMapper.writeValueAsString(Map.of(
                "from", "2026-03-02T06జ్ఞ\u200cా:24:07.180279Z",
                "to", "2026-03-02T07:24:07.180279Z"
        ));
        String encrypted = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/cats/catsOrg/booking/resources/R-001/schedules")
                        .contentType("application/json")
                        .content("{\"data\":\"" + encrypted + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void extremePositivePageShouldReturn400() throws Exception {
        mockMvc.perform(post("/cats/catsOrg/booking/resources")
                        .contentType("application/json")
                        .content("{\"page\":9223372036854775807,\"size\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void malformedJsonBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/cats/catsOrg/booking/reservations")
                        .contentType("application/json")
                        .content("\"{\"unexpected\" $ \"token\": \"value\"}\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void orgAReservationLifecycleShouldSucceed() throws Exception {
        String reservationId = objectMapper.readTree(
                mockMvc.perform(post("/cats/orgA/reservation/reservations")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "resourceId": "R-001",
                                          "scheduleId": "R-001-2026-02-26-9",
                                          "userId": "orga-user",
                                          "quantity": 1,
                                          "memo": "orgA smoke"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("CREATED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("reservationId").asText();

        mockMvc.perform(get("/cats/orgA/reservation/reservations/{reservationId}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(post("/cats/orgA/reservation/reservations/{reservationId}/cancel", reservationId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "reason": "user canceled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void orgBVisitLifecycleShouldSucceed() throws Exception {
        String visitId = objectMapper.readTree(
                mockMvc.perform(post("/cats/orgB/visit/visits")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "siteId": "SITE-01",
                                          "visitDate": "2026-02-26",
                                          "visitorName": "Kim Visitor",
                                          "visitorPhone": "010-1234-5678",
                                          "partySize": 2,
                                          "purpose": "demo"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("REQUESTED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("visitId").asText();

        mockMvc.perform(get("/cats/orgB/visit/visits/{visitId}", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId))
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        mockMvc.perform(post("/cats/orgB/visit/visits/{visitId}/cancel", visitId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "reason": "schedule changed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void orgBSupportTicketLifecycleShouldSucceed() throws Exception {
        String ticketId = objectMapper.readTree(
                mockMvc.perform(post("/cats/orgB/support/tickets")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "deviceId": "DEV-01",
                                          "requesterId": "ops-user",
                                          "issueType": "DISPLAY_ERROR",
                                          "description": "screen is blank"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("OPEN"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("ticketId").asText();

        mockMvc.perform(get("/cats/orgB/support/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(post("/cats/orgB/support/tickets/{ticketId}/resolve", ticketId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "resolutionNote": "restarted device"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
