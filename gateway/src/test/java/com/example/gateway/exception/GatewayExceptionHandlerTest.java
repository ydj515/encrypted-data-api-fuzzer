package com.example.gateway.exception;

import com.example.gateway.controller.GenericGatewayController;
import com.example.gateway.service.GatewayProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GenericGatewayController.class)
class GatewayExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayProxyService gatewayProxyService;

    @Test
    void shouldReturn400WhenIllegalArgumentExceptionOccurs() throws Exception {
        when(gatewayProxyService.proxyPost(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Missing template variable: resourceId"));

        mockMvc.perform(post("/cats/catsOrg/booking/getResourceInventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-02\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing template variable: resourceId"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void shouldReturn405WhenMethodIsNotAllowed() throws Exception {
        mockMvc.perform(get("/cats/catsOrg/booking/getResourceInventory"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("HTTP method not supported: GET"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void shouldReturn503WhenUpstreamIsUnavailable() throws Exception {
        when(gatewayProxyService.proxyPost(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("I/O error on POST request"));

        mockMvc.perform(post("/cats/catsOrg/booking/createReservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"R-001\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("UPSTREAM_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Upstream service is unavailable"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void shouldReturn404WhenMalformedPathDoesNotMatchHandler() throws Exception {
        mockMvc.perform(post(URI.create("/cats/catsOrg?/booking/createReservation"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"R-001\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No handler found for requested path"))
                .andExpect(jsonPath("$.traceId").isString());
    }
}
