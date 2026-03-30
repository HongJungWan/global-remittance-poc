package com.remittance.remittance.domain;

import com.remittance.remittance.domain.vo.ReceiverInfo;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RemittanceOrderTest {

    private RemittanceOrder order;

    @BeforeEach
    void setUp() {
        order = RemittanceOrder.create(
                UUID.randomUUID(),
                new ReceiverInfo("홍길동", "1234567890", "BANK001", "PH"),
                "KRW", "PHP",
                new BigDecimal("1000000.00"));
    }

    @Nested
    @DisplayName("생성")
    class Creation {
        @Test
        @DisplayName("주문 생성 시 CREATED 상태이다")
        void create_setsCreatedStatus() {
            assertEquals(RemittanceStatus.CREATED, order.getStatus());
            assertNotNull(order.getId());
            assertEquals(BigDecimal.ZERO, order.getExchangeRate());
            assertEquals(BigDecimal.ZERO, order.getTargetAmount());
        }
    }

    @Nested
    @DisplayName("유효한 상태 전이")
    class ValidTransitions {

        @Test
        @DisplayName("CREATED → QUOTE_LOCKED: lockQuote 성공")
        void lockQuote_success() {
            order.lockQuote(new BigDecimal("0.10"), new BigDecimal("100000.00"),
                    Instant.now().plusSeconds(30));
            assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());
            assertEquals(new BigDecimal("0.10"), order.getExchangeRate());
        }

        @Test
        @DisplayName("QUOTE_LOCKED → QUOTE_EXPIRED: expireQuote 성공")
        void expireQuote_success() {
            lockOrder();
            order.expireQuote();
            assertEquals(RemittanceStatus.QUOTE_EXPIRED, order.getStatus());
        }

        @Test
        @DisplayName("QUOTE_LOCKED → PAYMENT_PENDING: requestPayment 성공")
        void requestPayment_success() {
            lockOrder();
            order.requestPayment();
            assertEquals(RemittanceStatus.PAYMENT_PENDING, order.getStatus());
        }

        @Test
        @DisplayName("PAYMENT_PENDING → PAYMENT_COMPLETED: completePayment 성공")
        void completePayment_success() {
            moveTo(RemittanceStatus.PAYMENT_PENDING);
            order.completePayment();
            assertEquals(RemittanceStatus.PAYMENT_COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("PAYMENT_PENDING → PAYMENT_FAILED: failPayment 성공")
        void failPayment_success() {
            moveTo(RemittanceStatus.PAYMENT_PENDING);
            order.failPayment();
            assertEquals(RemittanceStatus.PAYMENT_FAILED, order.getStatus());
        }

        @Test
        @DisplayName("PAYMENT_FAILED → CANCELLED: cancel 성공 (보상 트랜잭션)")
        void cancel_afterPaymentFailed() {
            moveTo(RemittanceStatus.PAYMENT_PENDING);
            order.failPayment();
            order.cancel();
            assertEquals(RemittanceStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED → COMPLIANCE_CHECK: checkCompliance 성공")
        void checkCompliance_success() {
            moveTo(RemittanceStatus.PAYMENT_COMPLETED);
            order.checkCompliance();
            assertEquals(RemittanceStatus.COMPLIANCE_CHECK, order.getStatus());
        }

        @Test
        @DisplayName("COMPLIANCE_CHECK → PARTNER_PROCESSING: passCompliance 성공")
        void passCompliance_success() {
            moveTo(RemittanceStatus.COMPLIANCE_CHECK);
            order.passCompliance();
            assertEquals(RemittanceStatus.PARTNER_PROCESSING, order.getStatus());
        }

        @Test
        @DisplayName("COMPLIANCE_CHECK → COMPLIANCE_REJECTED: rejectCompliance 성공")
        void rejectCompliance_success() {
            moveTo(RemittanceStatus.COMPLIANCE_CHECK);
            order.rejectCompliance();
            assertEquals(RemittanceStatus.COMPLIANCE_REJECTED, order.getStatus());
        }

        @Test
        @DisplayName("COMPLIANCE_REJECTED → REFUND_PENDING: requestRefund 성공")
        void requestRefund_afterComplianceRejected() {
            moveTo(RemittanceStatus.COMPLIANCE_CHECK);
            order.rejectCompliance();
            order.requestRefund();
            assertEquals(RemittanceStatus.REFUND_PENDING, order.getStatus());
        }

        @Test
        @DisplayName("PARTNER_PROCESSING → COMPLETED: completeRemittance 성공")
        void completeRemittance_success() {
            moveTo(RemittanceStatus.PARTNER_PROCESSING);
            order.completeRemittance();
            assertEquals(RemittanceStatus.COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("PARTNER_PROCESSING → PARTNER_FAILED: failPartner 성공")
        void failPartner_success() {
            moveTo(RemittanceStatus.PARTNER_PROCESSING);
            order.failPartner();
            assertEquals(RemittanceStatus.PARTNER_FAILED, order.getStatus());
        }

        @Test
        @DisplayName("PARTNER_FAILED → REFUND_PENDING: requestRefund 성공 (보상 트랜잭션)")
        void requestRefund_afterPartnerFailed() {
            moveTo(RemittanceStatus.PARTNER_PROCESSING);
            order.failPartner();
            order.requestRefund();
            assertEquals(RemittanceStatus.REFUND_PENDING, order.getStatus());
        }

        @Test
        @DisplayName("REFUND_PENDING → REFUNDED: completeRefund 성공")
        void completeRefund_success() {
            moveTo(RemittanceStatus.PARTNER_PROCESSING);
            order.failPartner();
            order.requestRefund();
            order.completeRefund();
            assertEquals(RemittanceStatus.REFUNDED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("무효한 상태 전이 → IllegalStateException")
    class InvalidTransitions {

        @Test
        @DisplayName("CREATED에서 requestPayment 불가")
        void cannotRequestPaymentFromCreated() {
            assertThrows(IllegalStateException.class, () -> order.requestPayment());
        }

        @Test
        @DisplayName("CREATED에서 completePayment 불가")
        void cannotCompletePaymentFromCreated() {
            assertThrows(IllegalStateException.class, () -> order.completePayment());
        }

        @Test
        @DisplayName("QUOTE_LOCKED에서 completePayment 불가")
        void cannotCompletePaymentFromQuoteLocked() {
            lockOrder();
            assertThrows(IllegalStateException.class, () -> order.completePayment());
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED에서 cancel 불가")
        void cannotCancelFromPaymentCompleted() {
            moveTo(RemittanceStatus.PAYMENT_COMPLETED);
            assertThrows(IllegalStateException.class, () -> order.cancel());
        }

        @Test
        @DisplayName("COMPLETED에서 어떤 전이도 불가")
        void cannotTransitionFromCompleted() {
            moveTo(RemittanceStatus.COMPLETED);
            assertThrows(IllegalStateException.class, () -> order.lockQuote(BigDecimal.ONE, BigDecimal.ONE, Instant.now()));
            assertThrows(IllegalStateException.class, () -> order.requestPayment());
            assertThrows(IllegalStateException.class, () -> order.completePayment());
            assertThrows(IllegalStateException.class, () -> order.failPartner());
        }

        @Test
        @DisplayName("PAYMENT_PENDING에서 requestRefund 불가")
        void cannotRequestRefundFromPaymentPending() {
            moveTo(RemittanceStatus.PAYMENT_PENDING);
            assertThrows(IllegalStateException.class, () -> order.requestRefund());
        }

        @Test
        @DisplayName("CREATED에서 completeRemittance 불가")
        void cannotCompleteRemittanceFromCreated() {
            assertThrows(IllegalStateException.class, () -> order.completeRemittance());
        }
    }

    @Nested
    @DisplayName("환율 TTL 만료")
    class QuoteTTL {

        @Test
        @DisplayName("TTL 만료된 상태에서 requestPayment 시 QUOTE_EXPIRED로 전이 + 예외")
        void requestPayment_withExpiredQuote_expiresAndThrows() {
            order.lockQuote(new BigDecimal("0.10"), new BigDecimal("100000.00"),
                    Instant.now().minusSeconds(1)); // 이미 만료

            assertThrows(IllegalStateException.class, () -> order.requestPayment());
            assertEquals(RemittanceStatus.QUOTE_EXPIRED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("보상 트랜잭션 전체 흐름")
    class CompensationFlows {

        @Test
        @DisplayName("결제 실패 → 보상: PAYMENT_FAILED → CANCELLED")
        void paymentFailed_compensation() {
            moveTo(RemittanceStatus.PAYMENT_PENDING);
            order.failPayment();
            assertEquals(RemittanceStatus.PAYMENT_FAILED, order.getStatus());
            order.cancel();
            assertEquals(RemittanceStatus.CANCELLED, order.getStatus());
            assertTrue(order.getStatus().isTerminal());
        }

        @Test
        @DisplayName("파트너 실패 → 보상: PARTNER_FAILED → REFUND_PENDING → REFUNDED")
        void partnerFailed_compensation() {
            moveTo(RemittanceStatus.PARTNER_PROCESSING);
            order.failPartner();
            assertEquals(RemittanceStatus.PARTNER_FAILED, order.getStatus());
            order.requestRefund();
            assertEquals(RemittanceStatus.REFUND_PENDING, order.getStatus());
            order.completeRefund();
            assertEquals(RemittanceStatus.REFUNDED, order.getStatus());
            assertTrue(order.getStatus().isTerminal());
        }

        @Test
        @DisplayName("컴플라이언스 거부 → 보상: COMPLIANCE_REJECTED → REFUND_PENDING → REFUNDED")
        void complianceRejected_compensation() {
            moveTo(RemittanceStatus.COMPLIANCE_CHECK);
            order.rejectCompliance();
            order.requestRefund();
            order.completeRefund();
            assertEquals(RemittanceStatus.REFUNDED, order.getStatus());
            assertTrue(order.getStatus().isTerminal());
        }
    }

    @Nested
    @DisplayName("Happy Path 전체 흐름")
    class HappyPath {

        @Test
        @DisplayName("CREATED → QUOTE_LOCKED → PAYMENT_PENDING → PAYMENT_COMPLETED → COMPLIANCE_CHECK → PARTNER_PROCESSING → COMPLETED")
        void fullHappyPath() {
            assertEquals(RemittanceStatus.CREATED, order.getStatus());

            order.lockQuote(new BigDecimal("0.10"), new BigDecimal("100000.00"),
                    Instant.now().plusSeconds(30));
            assertEquals(RemittanceStatus.QUOTE_LOCKED, order.getStatus());

            order.requestPayment();
            assertEquals(RemittanceStatus.PAYMENT_PENDING, order.getStatus());

            order.completePayment();
            assertEquals(RemittanceStatus.PAYMENT_COMPLETED, order.getStatus());

            order.checkCompliance();
            assertEquals(RemittanceStatus.COMPLIANCE_CHECK, order.getStatus());

            order.passCompliance();
            assertEquals(RemittanceStatus.PARTNER_PROCESSING, order.getStatus());

            order.completeRemittance();
            assertEquals(RemittanceStatus.COMPLETED, order.getStatus());
            assertTrue(order.getStatus().isTerminal());
        }
    }

    // ── 헬퍼 ──

    private void lockOrder() {
        order.lockQuote(new BigDecimal("0.10"), new BigDecimal("100000.00"),
                Instant.now().plusSeconds(30));
    }

    private void moveTo(RemittanceStatus target) {
        switch (target) {
            case QUOTE_LOCKED -> lockOrder();
            case PAYMENT_PENDING -> { lockOrder(); order.requestPayment(); }
            case PAYMENT_COMPLETED -> { moveTo(RemittanceStatus.PAYMENT_PENDING); order.completePayment(); }
            case COMPLIANCE_CHECK -> { moveTo(RemittanceStatus.PAYMENT_COMPLETED); order.checkCompliance(); }
            case PARTNER_PROCESSING -> { moveTo(RemittanceStatus.COMPLIANCE_CHECK); order.passCompliance(); }
            case COMPLETED -> { moveTo(RemittanceStatus.PARTNER_PROCESSING); order.completeRemittance(); }
            default -> throw new UnsupportedOperationException("moveTo not supported for: " + target);
        }
    }
}
