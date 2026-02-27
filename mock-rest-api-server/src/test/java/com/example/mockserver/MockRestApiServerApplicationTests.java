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
}
