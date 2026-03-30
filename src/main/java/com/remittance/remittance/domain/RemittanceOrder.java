package com.remittance.remittance.domain;

import com.remittance.remittance.domain.vo.Money;
import com.remittance.remittance.domain.vo.ReceiverInfo;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 송금 주문 Aggregate Root.
 * 14개 상태의 상태 머신을 내장하며, 모든 전이는 이 엔티티의 메서드에서만 수행된다.
 */
@Entity
@Table(name = "remittance_orders", schema = "fintech_remittance")
public class RemittanceOrder {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Embedded
    private ReceiverInfo receiverInfo;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @Column(name = "source_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal sourceAmount;

    @Column(name = "target_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "quote_expires_at")
    private Instant quoteExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RemittanceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RemittanceOrder() {
    }

    /**
     * 송금 주문 생성 (CREATED 상태).
     * 환율 Lock-in 전이므로 환율/금액은 0으로 초기화된다.
     */
    public static RemittanceOrder create(UUID senderId, ReceiverInfo receiverInfo,
                                          String sourceCurrency, String targetCurrency,
                                          BigDecimal sourceAmount) {
        RemittanceOrder order = new RemittanceOrder();
        order.id = UUID.randomUUID();
        order.senderId = senderId;
        order.receiverInfo = receiverInfo;
        order.sourceCurrency = sourceCurrency;
        order.targetCurrency = targetCurrency;
        order.sourceAmount = sourceAmount;
        order.targetAmount = BigDecimal.ZERO;
        order.exchangeRate = BigDecimal.ZERO;
        order.status = RemittanceStatus.CREATED;
        order.createdAt = Instant.now();
        order.updatedAt = Instant.now();
        return order;
    }

    // ── 상태 전이 메서드 ──

    /** CREATED → QUOTE_LOCKED */
    public void lockQuote(BigDecimal exchangeRate, BigDecimal targetAmount, Instant expiresAt) {
        assertStatus(RemittanceStatus.CREATED, "환율 확정");
        this.exchangeRate = exchangeRate;
        this.targetAmount = targetAmount;
        this.quoteExpiresAt = expiresAt;
        transition(RemittanceStatus.QUOTE_LOCKED);
    }

    /** QUOTE_LOCKED → QUOTE_EXPIRED */
    public void expireQuote() {
        assertStatus(RemittanceStatus.QUOTE_LOCKED, "환율 만료");
        transition(RemittanceStatus.QUOTE_EXPIRED);
    }

    /** QUOTE_LOCKED → PAYMENT_PENDING */
    public void requestPayment() {
        assertStatus(RemittanceStatus.QUOTE_LOCKED, "결제 요청");
        if (quoteExpiresAt != null && Instant.now().isAfter(quoteExpiresAt)) {
            expireQuote();
            throw new IllegalStateException("환율 TTL이 만료되었습니다. 재견적이 필요합니다.");
        }
        transition(RemittanceStatus.PAYMENT_PENDING);
    }

    /** PAYMENT_PENDING → PAYMENT_COMPLETED */
    public void completePayment() {
        assertStatus(RemittanceStatus.PAYMENT_PENDING, "결제 완료");
        transition(RemittanceStatus.PAYMENT_COMPLETED);
    }

    /** PAYMENT_PENDING → PAYMENT_FAILED */
    public void failPayment() {
        assertStatus(RemittanceStatus.PAYMENT_PENDING, "결제 실패");
        transition(RemittanceStatus.PAYMENT_FAILED);
    }

    /** PAYMENT_FAILED → CANCELLED (보상 트랜잭션) */
    public void cancel() {
        assertStatus(RemittanceStatus.PAYMENT_FAILED, "취소");
        transition(RemittanceStatus.CANCELLED);
    }

    /** PAYMENT_COMPLETED → COMPLIANCE_CHECK */
    public void checkCompliance() {
        assertStatus(RemittanceStatus.PAYMENT_COMPLETED, "컴플라이언스 검증");
        transition(RemittanceStatus.COMPLIANCE_CHECK);
    }

    /** COMPLIANCE_CHECK → PARTNER_PROCESSING */
    public void passCompliance() {
        assertStatus(RemittanceStatus.COMPLIANCE_CHECK, "컴플라이언스 통과");
        transition(RemittanceStatus.PARTNER_PROCESSING);
    }

    /** COMPLIANCE_CHECK → COMPLIANCE_REJECTED */
    public void rejectCompliance() {
        assertStatus(RemittanceStatus.COMPLIANCE_CHECK, "컴플라이언스 거부");
        transition(RemittanceStatus.COMPLIANCE_REJECTED);
    }

    /** COMPLIANCE_REJECTED | PARTNER_FAILED → REFUND_PENDING (보상 트랜잭션) */
    public void requestRefund() {
        if (status != RemittanceStatus.COMPLIANCE_REJECTED && status != RemittanceStatus.PARTNER_FAILED) {
            throw new IllegalStateException(
                    "환불 요청은 COMPLIANCE_REJECTED 또는 PARTNER_FAILED 상태에서만 가능합니다. 현재: " + status);
        }
        transition(RemittanceStatus.REFUND_PENDING);
    }

    /** PARTNER_PROCESSING → COMPLETED */
    public void completeRemittance() {
        assertStatus(RemittanceStatus.PARTNER_PROCESSING, "송금 완료");
        transition(RemittanceStatus.COMPLETED);
    }

    /** PARTNER_PROCESSING → PARTNER_FAILED */
    public void failPartner() {
        assertStatus(RemittanceStatus.PARTNER_PROCESSING, "파트너 실패");
        transition(RemittanceStatus.PARTNER_FAILED);
    }

    /** REFUND_PENDING → REFUNDED */
    public void completeRefund() {
        assertStatus(RemittanceStatus.REFUND_PENDING, "환불 완료");
        transition(RemittanceStatus.REFUNDED);
    }

    // ── 내부 헬퍼 ──

    private void assertStatus(RemittanceStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    action + "은(는) " + expected + " 상태에서만 가능합니다. 현재: " + status);
        }
    }

    private void transition(RemittanceStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    // ── Getters ──

    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public ReceiverInfo getReceiverInfo() { return receiverInfo; }
    public String getSourceCurrency() { return sourceCurrency; }
    public String getTargetCurrency() { return targetCurrency; }
    public BigDecimal getSourceAmount() { return sourceAmount; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public Instant getQuoteExpiresAt() { return quoteExpiresAt; }
    public RemittanceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
