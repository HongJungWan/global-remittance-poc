package com.remittance.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * CQRS Read Model.
 * User 모듈의 UserCreatedEvent/UserUpdatedEvent를 구독하여 유지되는 읽기 전용 스냅샷.
 */
@Entity
@Table(name = "user_snapshots", schema = "fintech_remittance")
public class UserSnapshot {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "kyc_status", nullable = false, length = 20)
    private String kycStatus;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSnapshot() {
    }

    public UserSnapshot(UUID userId, String displayName, String kycStatus) {
        this.userId = userId;
        this.displayName = displayName;
        this.kycStatus = kycStatus;
        this.updatedAt = Instant.now();
    }

    public void update(String displayName, String kycStatus) {
        this.displayName = displayName;
        this.kycStatus = kycStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getKycStatus() { return kycStatus; }
    public Instant getUpdatedAt() { return updatedAt; }
}
