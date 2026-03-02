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
        mockMvc.perform(get("/cats/testOrg/testService/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12));
    }

    @Test
    void dailyScheduleListShouldReturnAtLeastTenItems() throws Exception {
        mockMvc.perform(get("/cats/testOrg/testService/schedules/daily")
                        .queryParam("date", "2026-02-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12));
    }

    @Test
    void encryptedInventoryRequestShouldReturnEncryptedDataEnvelope() throws Exception {
        String plain = objectMapper.writeValueAsString(Map.of("date", "2026-02-26"));
        String encrypted = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/cats/testOrg/testService/resources/R-001/inventory")
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

        mockMvc.perform(post("/cats/testOrg/testService/reservations")
                        .contentType("application/json")
                        .content("{\"data\":\"" + encrypted + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void invalidQueryDateShouldReturn400() throws Exception {
        mockMvc.perform(get("/cats/testOrg/testService/schedules/daily")
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

        mockMvc.perform(post("/cats/testOrg/testService/resources/R-001/schedules")
                        .contentType("application/json")
                        .content("{\"data\":\"" + encrypted + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void extremePositivePageShouldReturn400() throws Exception {
        mockMvc.perform(post("/cats/testOrg/testService/resources")
                        .contentType("application/json")
                        .content("{\"page\":9223372036854775807,\"size\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void malformedJsonBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/cats/testOrg/testService/reservations")
                        .contentType("application/json")
                        .content("\"{\"unexpected\" $ \"token\": \"value\"}\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isString());
    }
}
