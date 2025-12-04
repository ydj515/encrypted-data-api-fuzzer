package com.example.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamClientTest {

    private UpstreamClient upstreamClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        upstreamClient = new UpstreamClient(builder);
    }

    @Test
    // 정상 POST 요청 시 본문/헤더 포함해 성공 응답을 받는 케이스
    void postReturnsResponseOnSuccess() {
        // given
        server.expect(MockRestRequestMatchers.requestTo("http://localhost/"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("X-Test", "v"))
                .andExpect(MockRestRequestMatchers.content().json("{\"k\":\"v\"}"))
                .andRespond(MockRestResponseCreators.withSuccess("ok", null));

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "v");

        // when
        ResponseEntity<String> result = upstreamClient.post(URI.create("http://localhost/"), headers, Map.of("k", "v"));

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("ok");
        server.verify();
    }

    @Test
    // Upstream 에러 응답을 받아 상태/헤더/바디를 그대로 매핑하는 케이스
    void postMapsRestClientResponseException() {
        // given
        HttpHeaders upstreamHeaders = new HttpHeaders();
        upstreamHeaders.add("X-Upstream", "1");
        server.expect(MockRestRequestMatchers.requestTo("http://localhost/err"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.I_AM_A_TEAPOT)
                        .headers(upstreamHeaders)
                        .body("err"));

        // when
        ResponseEntity<String> result = upstreamClient.post(URI.create("http://localhost/err"), HttpHeaders.EMPTY, Map.of());

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(418);
        assertThat(result.getBody()).isEqualTo("err");
        assertThat(result.getHeaders().getFirst("X-Upstream")).isEqualTo("1");
        server.verify();
    }
}
