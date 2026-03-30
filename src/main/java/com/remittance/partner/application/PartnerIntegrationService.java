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

    @CircuitBreaker(name = "partnerApi", fallbackMethod = "partnerApiFallback")
    @Retry(name = "partnerApi")
    public String callPartnerApi(UUID remittanceId, String partnerCode,
                                  BigDecimal amount, String currency) {
        return partnerClient.sendRemittance(remittanceId, partnerCode, amount, currency);
    }

    @SuppressWarnings("unused")
    private String partnerApiFallback(UUID remittanceId, String partnerCode,
                                       BigDecimal amount, String currency, Throwable t) {
        log.warn("Partner API fallback triggered: remittanceId={}, cause={}", remittanceId, t.getMessage());
        throw new RuntimeException("Partner API unavailable (circuit breaker open): " + t.getMessage(), t);
    }
}
