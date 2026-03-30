package com.remittance.partner.application;

import com.remittance.partner.domain.PartnerTransaction;
import com.remittance.partner.infrastructure.PartnerRepository;
import com.remittance.partner.infrastructure.mock.ConfigurableMockPartnerClient;
import com.remittance.shared.event.PartnerCompletedEvent;
import com.remittance.shared.event.PartnerFailedEvent;
import com.remittance.shared.outbox.OutboxEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PartnerIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(PartnerIntegrationService.class);
    private static final String SCHEMA = "fintech_partner";
    private static final String AGGREGATE_TYPE = "PartnerTransaction";

    private final PartnerRepository partnerRepository;
    private final ConfigurableMockPartnerClient partnerClient;
    private final OutboxEventPublisher outboxEventPublisher;

    public PartnerIntegrationService(PartnerRepository partnerRepository,
                                      ConfigurableMockPartnerClient partnerClient,
                                      OutboxEventPublisher outboxEventPublisher) {
        this.partnerRepository = partnerRepository;
        this.partnerClient = partnerClient;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    /**
     * 파트너사 송금 처리를 요청한다.
     * Resilience4j CircuitBreaker + Retry가 적용된다.
     */
    @Transactional
    public PartnerTransaction processPartnerRequest(UUID remittanceId, String partnerCode,
                                                     BigDecimal amount, String currency) {
        PartnerTransaction tx = PartnerTransaction.create(remittanceId, partnerCode, amount, currency);
        partnerRepository.save(tx);

        try {
            String partnerTxId = callPartnerApi(remittanceId, partnerCode, amount, currency);
            tx.complete(partnerTxId);
            partnerRepository.save(tx);

            outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, tx.getId(),
                    new PartnerCompletedEvent(remittanceId, partnerTxId, Instant.now()));

            log.info("Partner processing completed: remittanceId={}, partnerTxId={}",
                    remittanceId, partnerTxId);
        } catch (Exception e) {
            tx.fail(e.getMessage());
            partnerRepository.save(tx);

            outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, tx.getId(),
                    new PartnerFailedEvent(remittanceId, tx.getId().toString(), e.getMessage()));

            log.error("Partner processing failed: remittanceId={}, reason={}",
                    remittanceId, e.getMessage());
        }

        return tx;
    }

    /**
     * 파트너 API 호출 (동기 래퍼).
     * Resilience4j CircuitBreaker + Retry 적용.
     */
    @CircuitBreaker(name = "partnerApi", fallbackMethod = "partnerApiFallback")
    @Retry(name = "partnerApi")
    public String callPartnerApi(UUID remittanceId, String partnerCode,
                                  BigDecimal amount, String currency) {
        return partnerClient.sendRemittance(remittanceId, partnerCode, amount, currency);
    }

    /**
     * 파트너 API 비동기 호출.
     * Resilience4j TimeLimiter + CircuitBreaker + Retry 적용.
     * WebClient 논블로킹 호출을 CompletableFuture로 래핑한다.
     */
    @TimeLimiter(name = "partnerApi")
    @CircuitBreaker(name = "partnerApi", fallbackMethod = "partnerApiAsyncFallback")
    @Retry(name = "partnerApi")
    public CompletableFuture<String> callPartnerApiAsync(UUID remittanceId, String partnerCode,
                                                          BigDecimal amount, String currency) {
        return partnerClient.sendRemittanceAsync(remittanceId, partnerCode, amount, currency)
                .toFuture();
    }

    @SuppressWarnings("unused")
    private String partnerApiFallback(UUID remittanceId, String partnerCode,
                                       BigDecimal amount, String currency, Throwable t) {
        log.warn("Partner API fallback triggered: remittanceId={}, cause={}", remittanceId, t.getMessage());
        throw new RuntimeException("Partner API unavailable (circuit breaker open): " + t.getMessage(), t);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<String> partnerApiAsyncFallback(UUID remittanceId, String partnerCode,
                                                               BigDecimal amount, String currency, Throwable t) {
        log.warn("Partner API async fallback triggered: remittanceId={}, cause={}", remittanceId, t.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Partner API unavailable (circuit breaker open): " + t.getMessage(), t));
    }
}
