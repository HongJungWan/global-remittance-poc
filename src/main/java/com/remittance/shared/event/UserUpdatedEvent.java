package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record UserUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String displayName,
        String kycStatus
) implements DomainEvent {

    public UserUpdatedEvent(UUID userId, String displayName, String kycStatus) {
        this(UUID.randomUUID(), Instant.now(), userId, displayName, kycStatus);
    }
}
