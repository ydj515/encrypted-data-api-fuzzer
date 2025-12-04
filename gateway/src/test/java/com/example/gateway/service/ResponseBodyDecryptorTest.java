package com.example.gateway.service;

import com.example.gateway.config.ApiRoute;
import com.example.gateway.crypto.CryptoModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseBodyDecryptorTest {

    @Mock
    private CryptoModule cryptoModule;

    @InjectMocks
    private ResponseBodyDecryptor decryptor;

    private final ApiRoute route = new ApiRoute("o", "s", "a", "POST", "h", "/p", "secret", null, null);

    @Test
    // data 필드를 복호화하여 성공적으로 평문 반환
    void decryptsDataField() {
        // given
        when(cryptoModule.decrypt("secret", "enc")).thenReturn("plain");

        // when
        String result = decryptor.decrypt(route, "{\"data\":\"enc\"}");

        // then
        assertThat(result).isEqualTo("plain");
    }

    @Test
    // data 필드가 없으면 원본 바디를 그대로 반환
    void returnsRawWhenNoDataField() {
        // given
        String body = "{\"other\":\"value\"}";

        // when
        String result = decryptor.decrypt(route, body);

        // then
        assertThat(result).isEqualTo(body);
    }

    @Test
    // 빈 바디나 null이면 빈 문자열 반환 (예외 없이 처리)
    void returnsEmptyStringWhenBodyBlank() {
        // when
        String blankResult = decryptor.decrypt(route, " ");
        String nullResult = decryptor.decrypt(route, null);

        // then
        assertThat(blankResult).isEqualTo("");
        assertThat(nullResult).isEqualTo("");
    }

    @Test
    // 복호화 실패 시 예외 없이 원본 바디 반환
    void returnsRawWhenDecryptionFails() {
        // given
        when(cryptoModule.decrypt("secret", "enc")).thenThrow(new RuntimeException("boom"));
        String body = "{\"data\":\"enc\"}";

        // when
        String result = decryptor.decrypt(route, body);

        // then
        assertThat(result).isEqualTo(body);
    }
}
