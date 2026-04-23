package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayProxyServiceTest {

    @Mock
    private CryptoModule cryptoModule;
    @Mock
    private ChecksumModule checksumModule;
    @Mock
    private RouteResolver routeResolver;
    @Mock
    private GatewayHeaderFactory gatewayHeaderFactory;
    @Mock
    private UpstreamClient upstreamClient;
    @Mock
    private ResponseBodyDecryptor responseBodyDecryptor;

    private GatewayProxyService gatewayProxyService;

    private final ApiRoute route = new ApiRoute("o", "s", "a", "POST", "http://example.com", "/ext", "key", null, null, null);

    @BeforeEach
    void setUp() {
        gatewayProxyService = new GatewayProxyService(
                cryptoModule,
                checksumModule,
                routeResolver,
                gatewayHeaderFactory,
                upstreamClient,
                responseBodyDecryptor,
                new ObjectMapper()
        );
    }

    @Test
    // encrypt → checksum → upstream 호출 → decrypt 흐름이 정상 동작하는 성공 케이스
    void proxyPostEncryptsCallsUpstreamAndDecrypts() {
        // given
        when(routeResolver.resolve("o", "s", "a", "POST")).thenReturn(route);
        when(cryptoModule.encrypt("key", "plain")).thenReturn("encrypted");
        when(checksumModule.checksum("encrypted")).thenReturn("chk");

        HttpHeaders headers = new HttpHeaders();
        headers.add("h", "v");
        when(gatewayHeaderFactory.build(route, "chk")).thenReturn(headers);

        ResponseEntity<String> upstreamResponse = ResponseEntity.status(201).body("cipher");
        when(upstreamClient.post(any(URI.class), eq(headers), anyMap())).thenReturn(upstreamResponse);
        when(responseBodyDecryptor.decrypt(route, "cipher")).thenReturn("plain-response");

        // when
        ResponseEntity<String> result = gatewayProxyService.proxyPost("o", "s", "a", "plain");

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo("plain-response");

        verify(routeResolver).resolve("o", "s", "a", "POST");
        verify(cryptoModule).encrypt("key", "plain");
        verify(checksumModule).checksum("encrypted");
        verify(responseBodyDecryptor).decrypt(route, "cipher");

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(upstreamClient).post(any(URI.class), eq(headers), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsEntry("data", "encrypted");
    }

    @Test
    // externalPath 템플릿이 존재하면 body의 변수값을 치환해 업스트림 URI를 만든다.
    void proxyPostExpandsExternalPathTemplateFromBody() {
        ApiRoute templateRoute = new ApiRoute(
                "o", "s", "inventory", "POST", "http://example.com",
                "/resources/{resourceId}/inventory", "key", null, null, null
        );
        when(routeResolver.resolve("o", "s", "inventory", "POST")).thenReturn(templateRoute);
        when(cryptoModule.encrypt("key", "{\"resourceId\":\"R-100\"}")).thenReturn("encrypted");
        when(checksumModule.checksum("encrypted")).thenReturn("chk");

        HttpHeaders headers = new HttpHeaders();
        when(gatewayHeaderFactory.build(templateRoute, "chk")).thenReturn(headers);

        when(upstreamClient.post(any(URI.class), eq(headers), anyMap()))
                .thenReturn(ResponseEntity.ok("cipher"));
        when(responseBodyDecryptor.decrypt(templateRoute, "cipher")).thenReturn("plain-response");

        gatewayProxyService.proxyPost("o", "s", "inventory", "{\"resourceId\":\"R-100\"}");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(upstreamClient).post(uriCaptor.capture(), eq(headers), anyMap());
        assertThat(uriCaptor.getValue().toString()).isEqualTo("http://example.com/resources/R-100/inventory");
    }

    @Test
    // 템플릿 변수값이 body에 없으면 예외를 던진다.
    void proxyPostThrowsWhenTemplateVariableMissing() {
        ApiRoute templateRoute = new ApiRoute(
                "o", "s", "inventory", "POST", "http://example.com",
                "/resources/{resourceId}/inventory", "key", null, null, null
        );
        when(routeResolver.resolve("o", "s", "inventory", "POST")).thenReturn(templateRoute);

        assertThatThrownBy(() -> gatewayProxyService.proxyPost("o", "s", "inventory", "{\"foo\":\"bar\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing template variable: resourceId");
    }

    @Test
    // path 변수에 주입성 문자열이 들어오면 업스트림 호출 전에 400 대상 예외로 차단한다.
    void proxyPostThrowsWhenTemplateVariableHasInvalidFormat() {
        ApiRoute templateRoute = new ApiRoute(
                "o", "s", "inventory", "POST", "http://example.com",
                "/resources/{resourceId}/inventory", "key", null, null, null
        );
        when(routeResolver.resolve("o", "s", "inventory", "POST")).thenReturn(templateRoute);

        assertThatThrownBy(() -> gatewayProxyService.proxyPost("o", "s", "inventory", "{\"resourceId\":\"; ls -la\",\"date\":\"2026-03-02\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid path variable format: resourceId");
    }
}
