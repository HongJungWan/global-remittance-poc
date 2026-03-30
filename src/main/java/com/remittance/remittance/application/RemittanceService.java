package com.remittance.remittance.application;

import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.ExchangeRateSnapshot;
import com.remittance.remittance.domain.vo.ReceiverInfo;
import com.remittance.remittance.infrastructure.RemittanceOrderRepository;
import com.remittance.shared.event.*;
import com.remittance.shared.lock.DistributedLockManager;
import com.remittance.shared.lock.LockHandle;
import com.remittance.shared.outbox.OutboxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RemittanceService {

    private static final Logger log = LoggerFactory.getLogger(RemittanceService.class);
    private static final String SCHEMA = "fintech_remittance";
    private static final String AGGREGATE_TYPE = "RemittanceOrder";

    private final RemittanceOrderRepository remittanceOrderRepository;
    private final ExchangeRateService exchangeRateService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DistributedLockManager lockManager;

    public RemittanceService(RemittanceOrderRepository remittanceOrderRepository,
                             ExchangeRateService exchangeRateService,
                             OutboxEventPublisher outboxEventPublisher,
                             DistributedLockManager lockManager) {
        this.remittanceOrderRepository = remittanceOrderRepository;
        this.exchangeRateService = exchangeRateService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.lockManager = lockManager;
    }

    /**
     * 견적 생성: 환율 Lock-in + CREATED → QUOTE_LOCKED 전이.
     */
    @Transactional
    public RemittanceOrder createQuote(UUID senderId, ReceiverInfo receiverInfo,
                                       String sourceCurrency, String targetCurrency,
                                       BigDecimal sourceAmount, String paymentMethod) {
        String lockKey = DistributedLockManager.createAccountLockKey(senderId);
        LockHandle handle = lockManager.tryLock(lockKey, 10, TimeUnit.SECONDS)
                .orElseThrow(() -> new IllegalStateException("동시 송금 요청이 진행 중입니다. 잠시 후 다시 시도해주세요."));

        try {
            RemittanceOrder order = RemittanceOrder.create(
                    senderId, receiverInfo, sourceCurrency, targetCurrency, sourceAmount);

            ExchangeRateSnapshot rateSnapshot = exchangeRateService.getLockedRate(sourceCurrency, targetCurrency);
            BigDecimal targetAmount = rateSnapshot.calculateTargetAmount(sourceAmount);

            order.lockQuote(rateSnapshot.rate(), targetAmount, rateSnapshot.expiresAt());
            remittanceOrderRepository.save(order);

            // 이벤트 발행
            outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, order.getId(),
                    new RemittanceCreatedEvent(order.getId(), senderId, sourceAmount, sourceCurrency));
            outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, order.getId(),
                    new QuoteLockedEvent(order.getId(), rateSnapshot.rate(), rateSnapshot.expiresAt()));

            return order;
        } finally {
            lockManager.unlock(handle);
        }
    }

    /**
     * 견적 확인: TTL 검증 → QUOTE_LOCKED → PAYMENT_PENDING 전이 → PaymentRequestedEvent 발행.
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public RemittanceOrder confirmQuote(UUID orderId, String paymentMethod) {
        RemittanceOrder order = getOrder(orderId);

        try {
            order.requestPayment();
        } catch (IllegalStateException e) {
            // TTL 만료로 QUOTE_EXPIRED 전이가 발생한 경우, 상태 변경을 커밋 후 예외 전파
            remittanceOrderRepository.save(order);
            outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, order.getId(),
                    new QuoteExpiredEvent(order.getId()));
            throw e;
        }
        remittanceOrderRepository.save(order);

        outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, order.getId(),
                new PaymentRequestedEvent(order.getId(), order.getSourceAmount(),
                        order.getSourceCurrency(), paymentMethod));

        return order;
    }

    /**
     * 결제 완료 이벤트 처리: PAYMENT_PENDING → PAYMENT_COMPLETED → COMPLIANCE_CHECK.
     */
    @Transactional
    public void handlePaymentCompleted(UUID orderId) {
        RemittanceOrder order = getOrder(orderId);
        order.completePayment();
        // PoC: 컴플라이언스를 자동 통과 처리
        order.checkCompliance();
        order.passCompliance();
        remittanceOrderRepository.save(order);

        outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, order.getId(),
                new PartnerProcessingRequestedEvent(
                        order.getId(),
                        "DEFAULT_PARTNER",
                        order.getTargetAmount(),
                        order.getTargetCurrency(),
                        order.getReceiverInfo().getName()));
    }

    /**
     * 결제 실패 이벤트 처리: PAYMENT_PENDING → PAYMENT_FAILED → CANCELLED.
     */
    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        RemittanceOrder order = getOrder(orderId);
        order.failPayment();
        order.cancel();
        remittanceOrderRepository.save(order);
    }

    /**
     * 파트너 완료 이벤트 처리: PARTNER_PROCESSING → COMPLETED.
     */
    @Transactional
    public void handlePartnerCompleted(UUID orderId) {
        RemittanceOrder order = getOrder(orderId);
        order.completeRemittance();
        remittanceOrderRepository.save(order);
    }

    /**
     * 파트너 실패 이벤트 처리: PARTNER_PROCESSING → PARTNER_FAILED → REFUND_PENDING.
     */
    @Transactional
    public void handlePartnerFailed(UUID orderId) {
        RemittanceOrder order = getOrder(orderId);
        order.failPartner();
        order.requestRefund();
        remittanceOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public RemittanceOrder getOrder(UUID orderId) {
        return remittanceOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("송금 주문을 찾을 수 없습니다: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<RemittanceOrder> getOrdersBySender(UUID senderId) {
        return remittanceOrderRepository.findBySenderIdOrderByCreatedAtDesc(senderId);
    }
}
