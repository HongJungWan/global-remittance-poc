package com.remittance.remittance.application;

import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import com.remittance.remittance.infrastructure.RemittanceOrderRepository;
import com.remittance.shared.event.QuoteExpiredEvent;
import com.remittance.shared.outbox.OutboxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 환율 Lock-in TTL 만료를 감지하는 스케줄러.
 * QUOTE_LOCKED 상태이면서 quoteExpiresAt이 지난 주문을 QUOTE_EXPIRED로 전이한다.
 */
@Component
public class QuoteExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpiryScheduler.class);

    private final RemittanceOrderRepository remittanceOrderRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public QuoteExpiryScheduler(RemittanceOrderRepository remittanceOrderRepository,
                                OutboxEventPublisher outboxEventPublisher) {
        this.remittanceOrderRepository = remittanceOrderRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void expireQuotes() {
        List<RemittanceOrder> lockedOrders =
                remittanceOrderRepository.findByStatus(RemittanceStatus.QUOTE_LOCKED);

        Instant now = Instant.now();
        for (RemittanceOrder order : lockedOrders) {
            if (order.getQuoteExpiresAt() != null && now.isAfter(order.getQuoteExpiresAt())) {
                order.expireQuote();
                remittanceOrderRepository.save(order);

                outboxEventPublisher.publish("fintech_remittance", "RemittanceOrder",
                        order.getId(), new QuoteExpiredEvent(order.getId()));

                log.info("Quote expired: orderId={}", order.getId());
            }
        }
    }
}
