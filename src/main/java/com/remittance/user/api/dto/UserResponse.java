package com.remittance.user.api.dto;

import com.remittance.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String kycStatus,
        String role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().getValue(),
                user.getDisplayName(),
                user.getKycStatus().name(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
