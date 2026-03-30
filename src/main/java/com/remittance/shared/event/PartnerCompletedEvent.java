package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record PartnerCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String partnerTransactionId,
        Instant completedAt
) implements DomainEvent {

    public PartnerCompletedEvent(UUID orderId, String partnerTransactionId, Instant completedAt) {
        this(UUID.randomUUID(), Instant.now(), orderId, partnerTransactionId, completedAt);
    }
}
