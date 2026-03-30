package com.remittance.acceptance;

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
 * 시나리오 3: 결제 실패.
 * 송금 생성 → Lock-in → 결제 실패 → 보상 트랜잭션 → CANCELLED
 */
class PaymentFailureAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("결제 실패: 견적 확인 → 결제 실패 이벤트 → PAYMENT_FAILED → CANCELLED")
    void paymentFailure_compensationToCancelled() {
        // 1. 회원가입
        AuthService.AuthResult user = authService.register(
                "payfail@test.com", "password123", "결제실패테스트");
        UUID senderId = user.userId();

        // 2. 견적 생성 + Lock-in
        ReceiverInfo receiver = new ReceiverInfo("박영희", "5555555555", "BANK003", "PH");
        RemittanceOrder order = remittanceService.createQuote(
                senderId, receiver, "USD", "PHP",
                new BigDecimal("300.00"), "CREDIT_CARD");
        assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());

        // 3. 견적 확인 (→ PAYMENT_PENDING)
        remittanceService.confirmQuote(order.getId(), "CREDIT_CARD");
        RemittanceOrder pending = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.PAYMENT_PENDING, pending.getStatus());

        // 4. 결제 실패 이벤트 시뮬레이션 (→ PAYMENT_FAILED → CANCELLED)
        remittanceService.handlePaymentFailed(order.getId());

        // 5. 상태 확인: CANCELLED (보상 트랜잭션 완료)
        RemittanceOrder cancelled = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getStatus().isTerminal());
    }
}
