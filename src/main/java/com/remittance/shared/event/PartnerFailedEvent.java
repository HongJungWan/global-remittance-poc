package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

public record PartnerFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String partnerTransactionId,
        String failureReason
) implements DomainEvent {

    public PartnerFailedEvent(UUID orderId, String partnerTransactionId, String failureReason) {
        this(UUID.randomUUID(), Instant.now(), orderId, partnerTransactionId, failureReason);
    }
}
