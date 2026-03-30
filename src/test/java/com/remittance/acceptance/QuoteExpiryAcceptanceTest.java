package com.remittance.acceptance;

import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.ReceiverInfo;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import com.remittance.user.application.AuthService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 시나리오 2: 환율 만료.
 * 송금 생성 → 환율 Lock-in → TTL 만료 → QUOTE_EXPIRED → 재견적 필요
 */
class QuoteExpiryAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("환율 만료: 견적 생성 → TTL 만료 후 확인 → QUOTE_EXPIRED + 예외")
    void quoteExpiry_afterTTL_transitionsToExpired() {
        // 1. 회원가입
        AuthService.AuthResult user = authService.register(
                "expiry@test.com", "password123", "만료테스트");
        UUID senderId = user.userId();

        // 2. 견적 생성 (CREATED → QUOTE_LOCKED)
        ReceiverInfo receiver = new ReceiverInfo("김철수", "9876543210", "BANK002", "PH");
        RemittanceOrder order = remittanceService.createQuote(
                senderId, receiver, "USD", "PHP",
                new BigDecimal("200.00"), "CREDIT_CARD");

        assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());

        // 3. TTL을 강제로 만료시킴 (JDBC로 직접 UPDATE)
        Instant expiredTime = Instant.now().minusSeconds(10);
        jdbcTemplate.update(
                "UPDATE fintech_remittance.remittance_orders SET quote_expires_at = ? WHERE id = ?",
                Timestamp.from(expiredTime), order.getId());

        // 4. JPA 1차 캐시를 클리어하여 JDBC 변경 반영
        entityManager.clear();

        // 5. 견적 확인 시도 → TTL 만료로 QUOTE_EXPIRED + 예외
        assertThrows(IllegalStateException.class, () ->
                remittanceService.confirmQuote(order.getId(), "CREDIT_CARD"));

        // 6. 상태 확인: QUOTE_EXPIRED (터미널 상태)
        RemittanceOrder expired = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.QUOTE_EXPIRED, expired.getStatus());
        assertTrue(expired.getStatus().isTerminal());
    }
}
