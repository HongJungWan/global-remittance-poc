package com.remittance.user.domain;

import com.remittance.user.domain.vo.Email;
import com.remittance.user.domain.vo.KycStatus;
import com.remittance.user.domain.vo.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * User Aggregate Root.
 * 고객 계정 관리, KYC 상태 관리를 담당한다.
 */
@Entity
@Table(name = "users", schema = "fintech_user")
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Embedded
    private Email email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(Email email, String passwordHash, String displayName) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.kycStatus = KycStatus.PENDING;
        this.role = Role.CUSTOMER;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String displayName) {
        this.displayName = displayName;
        this.updatedAt = Instant.now();
    }

    public void verifyKyc() {
        if (this.kycStatus == KycStatus.VERIFIED) {
            throw new IllegalStateException("이미 KYC 인증이 완료된 사용자입니다.");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.updatedAt = Instant.now();
    }

    public void rejectKyc() {
        if (this.kycStatus == KycStatus.REJECTED) {
            throw new IllegalStateException("이미 KYC가 거부된 사용자입니다.");
        }
        this.kycStatus = KycStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
