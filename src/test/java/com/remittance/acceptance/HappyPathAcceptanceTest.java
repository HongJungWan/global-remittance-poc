package com.remittance.acceptance;

import com.remittance.partner.infrastructure.mock.MockConfig;
import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.ReceiverInfo;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import com.remittance.user.application.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 시나리오 1: Happy Path.
 * 송금 생성 → 환율 Lock-in → 결제 완료 → 컴플라이언스 통과 → 파트너 처리 → 완료
 */
class HappyPathAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("Happy Path: 회원가입 → 견적 생성 → 확인 → 결제 완료 → 파트너 완료 → COMPLETED")
    void fullHappyPath() {
        // 1. 회원가입
        AuthService.AuthResult user = authService.register(
                "happy@test.com", "password123", "행복한사용자");
        UUID senderId = user.userId();
        assertNotNull(senderId);

        // 2. 견적 생성 (CREATED → QUOTE_LOCKED)
        ReceiverInfo receiver = new ReceiverInfo("홍길동", "1234567890", "BANK001", "PH");
        RemittanceOrder order = remittanceService.createQuote(
                senderId, receiver, "USD", "PHP",
                new BigDecimal("100.00"), "BANK_TRANSFER");

        assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());
        assertTrue(order.getExchangeRate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(order.getTargetAmount().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(order.getQuoteExpiresAt());

        // 3. 견적 확인 (QUOTE_LOCKED → PAYMENT_PENDING)
        RemittanceOrder confirmed = remittanceService.confirmQuote(order.getId(), "BANK_TRANSFER");
        assertEquals(RemittanceStatus.PAYMENT_PENDING, confirmed.getStatus());

        // 4. 결제 완료 이벤트 시뮬레이션 (PAYMENT_PENDING → PAYMENT_COMPLETED → COMPLIANCE_CHECK → PARTNER_PROCESSING)
        remittanceService.handlePaymentCompleted(order.getId());
        RemittanceOrder afterPayment = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.PARTNER_PROCESSING, afterPayment.getStatus());

        // 5. 파트너 완료 이벤트 시뮬레이션 (PARTNER_PROCESSING → COMPLETED)
        remittanceService.handlePartnerCompleted(order.getId());
        RemittanceOrder completed = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.COMPLETED, completed.getStatus());
        assertTrue(completed.getStatus().isTerminal());
    }
}
