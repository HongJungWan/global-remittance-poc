package com.remittance.payment.application;

import com.remittance.payment.domain.Payment;
import com.remittance.payment.domain.vo.PaymentMethod;
import com.remittance.payment.infrastructure.PaymentRepository;
import com.remittance.shared.event.PaymentCompletedEvent;
import com.remittance.shared.event.PaymentFailedEvent;
import com.remittance.shared.outbox.OutboxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SCHEMA = "fintech_payment";
    private static final String AGGREGATE_TYPE = "Payment";

    private final PaymentRepository paymentRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxEventPublisher outboxEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    /**
     * PaymentRequestedEvent를 수신하여 결제를 생성하고 처리한다.
     * PoC에서는 결제를 즉시 성공 처리한다.
     */
    @Transactional
    public Payment processPayment(UUID remittanceId, BigDecimal amount, String currency,
                                   String paymentMethodStr) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.BANK_TRANSFER;
        }

        Payment payment = Payment.create(remittanceId, amount, currency, method);

        // PoC: 즉시 성공 처리 (실제 PG 연동 대체)
        String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.complete(transactionRef);
        paymentRepository.save(payment);

        outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, payment.getId(),
                new PaymentCompletedEvent(remittanceId, payment.getId(), transactionRef));

        log.info("Payment completed: paymentId={}, remittanceId={}, ref={}",
                payment.getId(), remittanceId, transactionRef);
        return payment;
    }

    /**
     * 결제 실패 처리 (테스트/시뮬레이션용).
     */
    @Transactional
    public Payment failPayment(UUID remittanceId, BigDecimal amount, String currency,
                                String paymentMethodStr, String failureReason) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.BANK_TRANSFER;
        }

        Payment payment = Payment.create(remittanceId, amount, currency, method);
        payment.fail(failureReason);
        paymentRepository.save(payment);

        outboxEventPublisher.publish(SCHEMA, AGGREGATE_TYPE, payment.getId(),
                new PaymentFailedEvent(remittanceId, payment.getId(), failureReason));

        log.info("Payment failed: paymentId={}, remittanceId={}, reason={}",
                payment.getId(), remittanceId, failureReason);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: " + paymentId));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByRemittanceId(UUID remittanceId) {
        return paymentRepository.findByRemittanceId(remittanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 송금의 결제를 찾을 수 없습니다: " + remittanceId));
    }
}
