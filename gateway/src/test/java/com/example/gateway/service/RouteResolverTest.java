package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.config.GatewayProperties;
import com.example.gateway.exception.RouteNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteResolverTest {

    @Test
    // 존재하는 라우트를 정상 조회 성공
    void resolveReturnsMatchingRoute() {
        // given
        GatewayProperties props = new GatewayProperties();
        props.setApis(List.of(new ApiRoute("o", "s", "a", "POST", "http://example.com", "/ext", "k", null, null)));
        RouteResolver resolver = new RouteResolver(props);

        // when
        ApiRoute route = resolver.resolve("o", "s", "a", "POST");

        // then
        assertThat(route.host()).isEqualTo("http://example.com");
        assertThat(route.externalPath()).isEqualTo("/ext");
    }

    @Test
    // 라우트가 없으면 예외 발생해야 함
    void resolveThrowsWhenMissing() {
        // given
        GatewayProperties props = new GatewayProperties();
        RouteResolver resolver = new RouteResolver(props);

        // when then
        assertThatThrownBy(() -> resolver.resolve("x", "y", "z", "POST"))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("Route not found");
    }
}
