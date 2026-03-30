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
 * 시나리오 4: 파트너 장애.
 * 송금 → 결제 완료 → 파트너 실패 (서킷 브레이커) → REFUND_PENDING → REFUNDED
 */
class PartnerFailureAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("파트너 장애: 결제 완료 → 파트너 실패 → PARTNER_FAILED → REFUND_PENDING")
    void partnerFailure_compensationToRefundPending() {
        // 1. 회원가입
        AuthService.AuthResult user = authService.register(
                "partnerfail@test.com", "password123", "파트너실패테스트");
        UUID senderId = user.userId();

        // 2. 견적 생성 + Lock-in
        ReceiverInfo receiver = new ReceiverInfo("이순신", "7777777777", "BANK004", "PH");
        RemittanceOrder order = remittanceService.createQuote(
                senderId, receiver, "USD", "PHP",
                new BigDecimal("500.00"), "BANK_TRANSFER");
        assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());

        // 3. 견적 확인 (→ PAYMENT_PENDING)
        remittanceService.confirmQuote(order.getId(), "BANK_TRANSFER");

        // 4. 결제 완료 (→ PAYMENT_COMPLETED → COMPLIANCE_CHECK → PARTNER_PROCESSING)
        remittanceService.handlePaymentCompleted(order.getId());
        RemittanceOrder processing = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.PARTNER_PROCESSING, processing.getStatus());

        // 5. 파트너 실패 이벤트 시뮬레이션 (→ PARTNER_FAILED → REFUND_PENDING)
        remittanceService.handlePartnerFailed(order.getId());

        // 6. 상태 확인: REFUND_PENDING (보상 트랜잭션 진행 중)
        RemittanceOrder refundPending = remittanceService.getOrder(order.getId());
        assertEquals(RemittanceStatus.REFUND_PENDING, refundPending.getStatus());
        assertFalse(refundPending.getStatus().isTerminal());
    }
}
