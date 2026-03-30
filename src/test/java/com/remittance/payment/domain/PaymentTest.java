package com.remittance.payment.domain;

import com.remittance.payment.domain.vo.PaymentMethod;
import com.remittance.payment.domain.vo.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.create(UUID.randomUUID(), new BigDecimal("1000.00"),
                "KRW", PaymentMethod.CREDIT_CARD);
    }

    @Nested
    @DisplayName("생성")
    class Creation {
        @Test
        @DisplayName("결제 생성 시 PENDING 상태이다")
        void create_setsPendingStatus() {
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            assertNotNull(payment.getId());
            assertEquals("KRW", payment.getCurrency());
            assertEquals(PaymentMethod.CREDIT_CARD, payment.getPaymentMethod());
            assertNull(payment.getTransactionRef());
            assertNull(payment.getFailureReason());
        }
    }

    @Nested
    @DisplayName("유효한 상태 전이")
    class ValidTransitions {
        @Test
        @DisplayName("PENDING → COMPLETED: complete 성공")
        void complete_success() {
            payment.complete("TXN-12345");
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
            assertEquals("TXN-12345", payment.getTransactionRef());
        }

        @Test
        @DisplayName("PENDING → FAILED: fail 성공")
        void fail_success() {
            payment.fail("잔액 부족");
            assertEquals(PaymentStatus.FAILED, payment.getStatus());
            assertEquals("잔액 부족", payment.getFailureReason());
        }

        @Test
        @DisplayName("COMPLETED → REFUNDED: refund 성공")
        void refund_success() {
            payment.complete("TXN-12345");
            payment.refund();
            assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        }
    }

    @Nested
    @DisplayName("무효한 상태 전이")
    class InvalidTransitions {
        @Test
        @DisplayName("COMPLETED에서 complete 불가")
        void cannotCompleteFromCompleted() {
            payment.complete("TXN-12345");
            assertThrows(IllegalStateException.class, () -> payment.complete("TXN-99999"));
        }

        @Test
        @DisplayName("FAILED에서 complete 불가")
        void cannotCompleteFromFailed() {
            payment.fail("오류");
            assertThrows(IllegalStateException.class, () -> payment.complete("TXN-12345"));
        }

        @Test
        @DisplayName("PENDING에서 refund 불가")
        void cannotRefundFromPending() {
            assertThrows(IllegalStateException.class, () -> payment.refund());
        }

        @Test
        @DisplayName("FAILED에서 refund 불가")
        void cannotRefundFromFailed() {
            payment.fail("오류");
            assertThrows(IllegalStateException.class, () -> payment.refund());
        }

        @Test
        @DisplayName("REFUNDED에서 어떤 전이도 불가")
        void cannotTransitionFromRefunded() {
            payment.complete("TXN-12345");
            payment.refund();
            assertThrows(IllegalStateException.class, () -> payment.complete("TXN-99999"));
            assertThrows(IllegalStateException.class, () -> payment.fail("오류"));
            assertThrows(IllegalStateException.class, () -> payment.refund());
        }
    }

    @Nested
    @DisplayName("터미널 상태")
    class TerminalStates {
        @Test
        @DisplayName("COMPLETED는 터미널 상태이다")
        void completed_isTerminal() {
            payment.complete("TXN-12345");
            assertTrue(payment.getStatus().isTerminal());
        }

        @Test
        @DisplayName("FAILED는 터미널 상태이다")
        void failed_isTerminal() {
            payment.fail("오류");
            assertTrue(payment.getStatus().isTerminal());
        }

        @Test
        @DisplayName("REFUNDED는 터미널 상태이다")
        void refunded_isTerminal() {
            payment.complete("TXN-12345");
            payment.refund();
            assertTrue(payment.getStatus().isTerminal());
        }

        @Test
        @DisplayName("PENDING은 터미널 상태가 아니다")
        void pending_isNotTerminal() {
            assertFalse(payment.getStatus().isTerminal());
        }
    }
}
