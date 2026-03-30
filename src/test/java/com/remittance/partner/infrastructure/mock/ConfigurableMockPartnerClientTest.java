package com.remittance.partner.infrastructure.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurableMockPartnerClientTest {

    private ConfigurableMockPartnerClient client;

    @BeforeEach
    void setUp() {
        client = new ConfigurableMockPartnerClient();
    }

    @Test
    @DisplayName("SUCCESS 모드: 파트너 트랜잭션 ID를 반환한다")
    void successMode_returnsTxId() {
        client.getConfig().setMode(MockConfig.Mode.SUCCESS);

        String txId = client.sendRemittance(UUID.randomUUID(), "PARTNER_A",
                new BigDecimal("1000.00"), "PHP");

        assertNotNull(txId);
        assertTrue(txId.startsWith("PTX-"));
    }

    @Test
    @DisplayName("FAILURE 모드: RuntimeException을 발생시킨다")
    void failureMode_throwsException() {
        client.getConfig().setMode(MockConfig.Mode.FAILURE);

        assertThrows(RuntimeException.class, () ->
                client.sendRemittance(UUID.randomUUID(), "PARTNER_A",
                        new BigDecimal("1000.00"), "PHP"));
    }

    @Test
    @DisplayName("TIMEOUT 모드: RuntimeException을 발생시킨다")
    void timeoutMode_throwsException() {
        client.getConfig().setMode(MockConfig.Mode.TIMEOUT);

        assertThrows(RuntimeException.class, () ->
                client.sendRemittance(UUID.randomUUID(), "PARTNER_A",
                        new BigDecimal("1000.00"), "PHP"));
    }

    @Test
    @DisplayName("RANDOM 모드 (실패율 0%): 항상 성공한다")
    void randomMode_zeroFailure_alwaysSucceeds() {
        client.getConfig().setMode(MockConfig.Mode.RANDOM);
        client.getConfig().setFailurePercent(0);

        for (int i = 0; i < 10; i++) {
            String txId = client.sendRemittance(UUID.randomUUID(), "PARTNER_A",
                    new BigDecimal("1000.00"), "PHP");
            assertNotNull(txId);
        }
    }

    @Test
    @DisplayName("RANDOM 모드 (실패율 100%): 항상 실패한다")
    void randomMode_fullFailure_alwaysFails() {
        client.getConfig().setMode(MockConfig.Mode.RANDOM);
        client.getConfig().setFailurePercent(100);

        for (int i = 0; i < 10; i++) {
            assertThrows(RuntimeException.class, () ->
                    client.sendRemittance(UUID.randomUUID(), "PARTNER_A",
                            new BigDecimal("1000.00"), "PHP"));
        }
    }

    @Test
    @DisplayName("기본 설정: SUCCESS 모드, 지연 0ms, 실패율 30%")
    void defaultConfig() {
        MockConfig config = client.getConfig();
        assertEquals(MockConfig.Mode.SUCCESS, config.getMode());
        assertEquals(0, config.getDelayMs());
        assertEquals(30, config.getFailurePercent());
    }
}
