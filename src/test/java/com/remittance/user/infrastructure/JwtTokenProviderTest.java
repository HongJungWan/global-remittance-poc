package com.remittance.user.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-jwt-secret-key-for-testing-only-must-be-at-least-32-bytes",
                900000,   // 15분
                604800000 // 7일
        );
    }

    @Test
    @DisplayName("Access Token이 정상적으로 생성된다")
    void createAccessToken_generatesValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("토큰에서 userId를 추출할 수 있다")
    void getUserId_extractsCorrectUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", "CUSTOMER");

        assertEquals(userId, jwtTokenProvider.getUserId(token));
    }

    @Test
    @DisplayName("토큰에서 role을 추출할 수 있다")
    void getRole_extractsCorrectRole() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", "ADMIN");

        assertEquals("ADMIN", jwtTokenProvider.getRole(token));
    }

    @Test
    @DisplayName("잘못된 토큰은 검증에 실패한다")
    void invalidToken_failsValidation() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("null 토큰은 검증에 실패한다")
    void nullToken_failsValidation() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Refresh Token이 정상적으로 생성된다")
    void createRefreshToken_generatesValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createRefreshToken(userId, "test@example.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserId(token));
    }

    @Test
    @DisplayName("Access Token과 Refresh Token은 서로 다른 토큰이다")
    void accessAndRefreshTokens_areDifferent() {
        UUID userId = UUID.randomUUID();
        String access = jwtTokenProvider.createAccessToken(userId, "test@example.com", "CUSTOMER");
        String refresh = jwtTokenProvider.createRefreshToken(userId, "test@example.com", "CUSTOMER");

        assertNotEquals(access, refresh);
    }
}
