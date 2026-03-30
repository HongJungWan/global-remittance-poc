package com.remittance.partner.domain;

import com.remittance.partner.domain.vo.PartnerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartnerTransactionTest {

    private PartnerTransaction tx;

    @BeforeEach
    void setUp() {
        tx = PartnerTransaction.create(UUID.randomUUID(), "PARTNER_A",
                new BigDecimal("1000.00"), "PHP");
    }

    @Nested
    @DisplayName("생성")
    class Creation {
        @Test
        @DisplayName("트랜잭션 생성 시 PROCESSING 상태이다")
        void create_setsProcessingStatus() {
            assertEquals(PartnerStatus.PROCESSING, tx.getStatus());
            assertNotNull(tx.getId());
            assertEquals("PARTNER_A", tx.getPartnerCode());
            assertNull(tx.getPartnerTransactionId());
            assertNull(tx.getFailureReason());
        }
    }

    @Nested
    @DisplayName("유효한 상태 전이")
    class ValidTransitions {
        @Test
        @DisplayName("PROCESSING → COMPLETED: complete 성공")
        void complete_success() {
            tx.complete("PTX-12345");
            assertEquals(PartnerStatus.COMPLETED, tx.getStatus());
            assertEquals("PTX-12345", tx.getPartnerTransactionId());
        }

        @Test
        @DisplayName("PROCESSING → FAILED: fail 성공")
        void fail_success() {
            tx.fail("Connection refused");
            assertEquals(PartnerStatus.FAILED, tx.getStatus());
            assertEquals("Connection refused", tx.getFailureReason());
        }

        @Test
        @DisplayName("PROCESSING → TIMEOUT: timeout 성공")
        void timeout_success() {
            tx.timeout();
            assertEquals(PartnerStatus.TIMEOUT, tx.getStatus());
            assertEquals("Partner API timeout", tx.getFailureReason());
        }
    }

    @Nested
    @DisplayName("무효한 상태 전이")
    class InvalidTransitions {
        @Test
        @DisplayName("COMPLETED에서 complete 불가")
        void cannotCompleteFromCompleted() {
            tx.complete("PTX-12345");
            assertThrows(IllegalStateException.class, () -> tx.complete("PTX-99999"));
        }

        @Test
        @DisplayName("FAILED에서 complete 불가")
        void cannotCompleteFromFailed() {
            tx.fail("오류");
            assertThrows(IllegalStateException.class, () -> tx.complete("PTX-12345"));
        }

        @Test
        @DisplayName("TIMEOUT에서 fail 불가")
        void cannotFailFromTimeout() {
            tx.timeout();
            assertThrows(IllegalStateException.class, () -> tx.fail("오류"));
        }

        @Test
        @DisplayName("COMPLETED에서 timeout 불가")
        void cannotTimeoutFromCompleted() {
            tx.complete("PTX-12345");
            assertThrows(IllegalStateException.class, () -> tx.timeout());
        }
    }

    @Nested
    @DisplayName("터미널 상태")
    class TerminalStates {
        @Test
        @DisplayName("COMPLETED, FAILED, TIMEOUT은 터미널 상태이다")
        void terminalStates() {
            assertTrue(PartnerStatus.COMPLETED.isTerminal());
            assertTrue(PartnerStatus.FAILED.isTerminal());
            assertTrue(PartnerStatus.TIMEOUT.isTerminal());
        }

        @Test
        @DisplayName("PROCESSING은 터미널 상태가 아니다")
        void processingIsNotTerminal() {
            assertFalse(PartnerStatus.PROCESSING.isTerminal());
        }
    }
}
