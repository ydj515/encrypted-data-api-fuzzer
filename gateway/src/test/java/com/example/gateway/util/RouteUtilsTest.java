package com.example.gateway.util;

import com.example.gateway.config.ApiRoute;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteUtilsTest {

    @Test
    // externalPath 앞에 슬래시가 없어도 URI를 정상 조립 (성공)
    void buildsUriWithLeadingSlashHandled() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "http://example.com", "ext/path", "k", null, null);

        // when
        URI uri = RouteUtils.buildTargetUri(route);

        // then
        assertThat(uri.toString()).isEqualTo("http://example.com/ext/path");
    }

    @Test
    // externalPath에 슬래시가 있어도 중복 없이 URI를 조립 (성공)
    void buildsUriWithExistingSlash() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "http://example.com", "/ext/path", "k", null, null);

        // when
        URI uri = RouteUtils.buildTargetUri(route);

        // then
        assertThat(uri.toString()).isEqualTo("http://example.com/ext/path");
    }

    @Test
    // host가 없으면 예외 발생 (실패 케이스 기대)
    void throwsWhenHostMissing() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "", "/p", "k", null, null);

        // when then
        assertThatThrownBy(() -> RouteUtils.buildTargetUri(route))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Host not configured");
    }

    @Test
    // externalPath가 없으면 예외 발생 (실패 케이스 기대)
    void throwsWhenPathMissing() {
        // given
        ApiRoute route = new ApiRoute("o", "s", "a", "POST", "http://example.com", " ", "k", null, null);

        // when then
        assertThatThrownBy(() -> RouteUtils.buildTargetUri(route))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("External path not configured");
    }
}
