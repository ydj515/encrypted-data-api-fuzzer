package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayHeaderFactoryTest {

    private final GatewayHeaderFactory factory = new GatewayHeaderFactory();

    @Test
    // 필수/옵션 헤더가 모두 세팅되는 성공 케이스
    void buildsHeadersWithOptionalValues() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "h", "/p", "k", "apiKey", "ins");

        // when
        HttpHeaders headers = factory.build(route, "chk");

        // then
        assertThat(headers.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(headers.getAccept()).containsExactly(MediaType.APPLICATION_JSON);
        assertThat(headers.getFirst(GatewayHeaderNames.X_CHECKSUM)).isEqualTo("chk");
        assertThat(headers.getFirst(GatewayHeaderNames.X_API_KEY)).isEqualTo("apiKey");
        assertThat(headers.getFirst(GatewayHeaderNames.X_INS_CODE)).isEqualTo("ins");
    }

    @Test
    // 옵션 값이 없으면 헤더를 생략하는 성공 케이스
    void skipsAbsentOptionalHeaders() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "h", "/p", "k", null, null);

        // when
        HttpHeaders headers = factory.build(route, "chk");

        // then
        assertThat(headers.containsKey(GatewayHeaderNames.X_API_KEY)).isFalse();
        assertThat(headers.containsKey(GatewayHeaderNames.X_INS_CODE)).isFalse();
        assertThat(headers.getFirst(GatewayHeaderNames.X_CHECKSUM)).isEqualTo("chk");
    }
}
