package com.remittance.payment.domain;

import com.remittance.payment.domain.vo.PaymentMethod;
import com.remittance.payment.domain.vo.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Aggregate Root.
 * 송금을 위한 결제 트랜잭션을 관리한다.
 * PaymentRequestedEvent를 구독하여 생성되고, 처리 결과를 이벤트로 발행한다.
 */
@Entity
@Table(name = "payments", schema = "fintech_payment")
public class Payment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "remittance_id", nullable = false)
    private UUID remittanceId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public static Payment create(UUID remittanceId, BigDecimal amount, String currency,
                                  PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.remittanceId = remittanceId;
        payment.amount = amount;
        payment.currency = currency;
        payment.paymentMethod = paymentMethod;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = Instant.now();
        payment.updatedAt = Instant.now();
        return payment;
    }

    /** PENDING → COMPLETED */
    public void complete(String transactionRef) {
        assertStatus(PaymentStatus.PENDING, "결제 완료");
        this.transactionRef = transactionRef;
        transition(PaymentStatus.COMPLETED);
    }

    /** PENDING → FAILED */
    public void fail(String failureReason) {
        assertStatus(PaymentStatus.PENDING, "결제 실패");
        this.failureReason = failureReason;
        transition(PaymentStatus.FAILED);
    }

    /** COMPLETED → REFUNDED */
    public void refund() {
        assertStatus(PaymentStatus.COMPLETED, "환불");
        transition(PaymentStatus.REFUNDED);
    }

    private void assertStatus(PaymentStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    action + "은(는) " + expected + " 상태에서만 가능합니다. 현재: " + status);
        }
    }

    private void transition(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRemittanceId() { return remittanceId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
