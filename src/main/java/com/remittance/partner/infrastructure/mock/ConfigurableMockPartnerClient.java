package com.remittance.partner.infrastructure.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 파트너사 API Configurable Mock.
 * 동작 모드에 따라 SUCCESS, FAILURE, TIMEOUT, RANDOM 응답을 시뮬레이션한다.
 */
@Component
public class ConfigurableMockPartnerClient {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableMockPartnerClient.class);

    private final MockConfig config = new MockConfig();

    public MockConfig getConfig() {
        return config;
    }

    /**
     * 파트너사 송금 요청을 시뮬레이션한다.
     *
     * @return 파트너 트랜잭션 ID (성공 시), null (실패/타임아웃 시 예외 발생)
     */
    public String sendRemittance(UUID remittanceId, String partnerCode,
                                  BigDecimal amount, String currency) {
        simulateDelay();

        MockConfig.Mode effectiveMode = resolveMode();

        return switch (effectiveMode) {
            case SUCCESS -> {
                String txId = "PTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.info("Mock partner SUCCESS: remittanceId={}, txId={}", remittanceId, txId);
                yield txId;
            }
            case FAILURE -> {
                log.info("Mock partner FAILURE: remittanceId={}", remittanceId);
                throw new RuntimeException("Partner API failure (mock): Service unavailable");
            }
            case TIMEOUT -> {
                log.info("Mock partner TIMEOUT: remittanceId={}", remittanceId);
                throw new RuntimeException("Partner API timeout (mock): Request timed out after 5000ms");
            }
            case RANDOM -> {
                boolean shouldFail = ThreadLocalRandom.current().nextInt(100) < config.getFailurePercent();
                if (shouldFail) {
                    log.info("Mock partner RANDOM → FAILURE: remittanceId={}", remittanceId);
                    throw new RuntimeException("Partner API failure (mock/random)");
                }
                String txId = "PTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.info("Mock partner RANDOM → SUCCESS: remittanceId={}, txId={}", remittanceId, txId);
                yield txId;
            }
        };
    }

    private MockConfig.Mode resolveMode() {
        return config.getMode();
    }

    private void simulateDelay() {
        long delay = config.getDelayMs();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
