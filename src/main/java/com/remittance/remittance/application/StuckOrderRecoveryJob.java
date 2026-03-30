package com.remittance.remittance.application;

import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import com.remittance.remittance.infrastructure.RemittanceOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Stuck 상태 복구 Job.
 * 5분 주기로 타임아웃된 주문을 감지하여 실패 처리 + 보상 트랜잭션을 수행한다.
 *
 * - PAYMENT_PENDING > 10분 → failPayment() + cancel()
 * - PARTNER_PROCESSING > 15분 → failPartner() + requestRefund()
 */
@Component
public class StuckOrderRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(StuckOrderRecoveryJob.class);

    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration PARTNER_TIMEOUT = Duration.ofMinutes(15);

    private final RemittanceOrderRepository remittanceOrderRepository;

    public StuckOrderRecoveryJob(RemittanceOrderRepository remittanceOrderRepository) {
        this.remittanceOrderRepository = remittanceOrderRepository;
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void recoverStuckOrders() {
        Instant now = Instant.now();

        // PAYMENT_PENDING 타임아웃
        List<RemittanceOrder> stuckPayments = remittanceOrderRepository
                .findByStatusAndUpdatedAtBefore(RemittanceStatus.PAYMENT_PENDING,
                        now.minus(PAYMENT_TIMEOUT));

        for (RemittanceOrder order : stuckPayments) {
            log.warn("Stuck PAYMENT_PENDING detected: orderId={}, updatedAt={}",
                    order.getId(), order.getUpdatedAt());
            order.failPayment();
            order.cancel();
            remittanceOrderRepository.save(order);
        }

        // PARTNER_PROCESSING 타임아웃
        List<RemittanceOrder> stuckPartner = remittanceOrderRepository
                .findByStatusAndUpdatedAtBefore(RemittanceStatus.PARTNER_PROCESSING,
                        now.minus(PARTNER_TIMEOUT));

        for (RemittanceOrder order : stuckPartner) {
            log.warn("Stuck PARTNER_PROCESSING detected: orderId={}, updatedAt={}",
                    order.getId(), order.getUpdatedAt());
            order.failPartner();
            order.requestRefund();
            remittanceOrderRepository.save(order);
        }
    }
}
