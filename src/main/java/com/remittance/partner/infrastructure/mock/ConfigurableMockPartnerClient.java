package com.remittance.partner.infrastructure.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 파트너사 API Configurable Mock (WebClient 비동기 논블로킹).
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
     * 파트너사 송금 요청을 비동기로 시뮬레이션한다.
     *
     * @return Mono<String> 파트너 트랜잭션 ID (성공 시), error signal (실패/타임아웃 시)
     */
    public Mono<String> sendRemittanceAsync(UUID remittanceId, String partnerCode,
                                             BigDecimal amount, String currency) {
        Mono<String> result = Mono.defer(() -> {
            MockConfig.Mode effectiveMode = resolveMode();

            return switch (effectiveMode) {
                case SUCCESS -> {
                    String txId = "PTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    log.info("Mock partner SUCCESS: remittanceId={}, txId={}", remittanceId, txId);
                    yield Mono.just(txId);
                }
                case FAILURE -> {
                    log.info("Mock partner FAILURE: remittanceId={}", remittanceId);
                    yield Mono.error(new RuntimeException("Partner API failure (mock): Service unavailable"));
                }
                case TIMEOUT -> {
                    log.info("Mock partner TIMEOUT: remittanceId={}", remittanceId);
                    yield Mono.error(new RuntimeException("Partner API timeout (mock): Request timed out after 5000ms"));
                }
                case RANDOM -> {
                    boolean shouldFail = ThreadLocalRandom.current().nextInt(100) < config.getFailurePercent();
                    if (shouldFail) {
                        log.info("Mock partner RANDOM → FAILURE: remittanceId={}", remittanceId);
                        yield Mono.error(new RuntimeException("Partner API failure (mock/random)"));
                    }
                    String txId = "PTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    log.info("Mock partner RANDOM → SUCCESS: remittanceId={}, txId={}", remittanceId, txId);
                    yield Mono.just(txId);
                }
            };
        });

        long delay = config.getDelayMs();
        if (delay > 0) {
            result = result.delaySubscription(Duration.ofMillis(delay));
        }

        return result;
    }

    /**
     * 동기 호출 (하위 호환 및 Resilience4j 연동용).
     * 내부적으로 비동기 Mono를 block()하여 결과를 반환한다.
     */
    public String sendRemittance(UUID remittanceId, String partnerCode,
                                  BigDecimal amount, String currency) {
        return sendRemittanceAsync(remittanceId, partnerCode, amount, currency).block();
    }

    private MockConfig.Mode resolveMode() {
        return config.getMode();
    }
}
