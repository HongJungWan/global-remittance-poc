package com.remittance.partner.domain;

import com.remittance.partner.domain.vo.PartnerStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Partner Transaction Aggregate Root.
 * 외부 파트너사 송금 처리를 추적한다.
 * Anti-Corruption Layer(ACL)로서 외부 모델이 내부 도메인을 오염시키지 않도록 격리한다.
 */
@Entity
@Table(name = "partner_transactions", schema = "fintech_partner")
public class PartnerTransaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "remittance_id", nullable = false)
    private UUID remittanceId;

    @Column(name = "partner_code", nullable = false, length = 30)
    private String partnerCode;

    @Column(name = "partner_transaction_id", length = 100)
    private String partnerTransactionId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PartnerTransaction() {
    }

    public static PartnerTransaction create(UUID remittanceId, String partnerCode,
                                             BigDecimal amount, String currency) {
        PartnerTransaction tx = new PartnerTransaction();
        tx.id = UUID.randomUUID();
        tx.remittanceId = remittanceId;
        tx.partnerCode = partnerCode;
        tx.amount = amount;
        tx.currency = currency;
        tx.status = PartnerStatus.PROCESSING;
        tx.createdAt = Instant.now();
        tx.updatedAt = Instant.now();
        return tx;
    }

    /** PROCESSING → COMPLETED */
    public void complete(String partnerTransactionId) {
        assertStatus(PartnerStatus.PROCESSING, "완료");
        this.partnerTransactionId = partnerTransactionId;
        transition(PartnerStatus.COMPLETED);
    }

    /** PROCESSING → FAILED */
    public void fail(String failureReason) {
        assertStatus(PartnerStatus.PROCESSING, "실패");
        this.failureReason = failureReason;
        transition(PartnerStatus.FAILED);
    }

    /** PROCESSING → TIMEOUT */
    public void timeout() {
        assertStatus(PartnerStatus.PROCESSING, "타임아웃");
        this.failureReason = "Partner API timeout";
        transition(PartnerStatus.TIMEOUT);
    }

    private void assertStatus(PartnerStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    action + "은(는) " + expected + " 상태에서만 가능합니다. 현재: " + status);
        }
    }

    private void transition(PartnerStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRemittanceId() { return remittanceId; }
    public String getPartnerCode() { return partnerCode; }
    public String getPartnerTransactionId() { return partnerTransactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PartnerStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
