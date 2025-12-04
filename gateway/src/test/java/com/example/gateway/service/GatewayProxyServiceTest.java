package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
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

    private final ApiRoute route = new ApiRoute("o", "s", "a", "POST", "http://example.com", "/ext", "key", null, null);

    @BeforeEach
    void setUp() {
        gatewayProxyService = new GatewayProxyService(
                cryptoModule,
                checksumModule,
                routeResolver,
                gatewayHeaderFactory,
                upstreamClient,
                responseBodyDecryptor
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
}
