package com.remittance.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartnerProcessingRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String partnerCode,
        BigDecimal amount,
        String targetCurrency,
        String receiverInfo
) implements DomainEvent {

    public PartnerProcessingRequestedEvent(UUID orderId, String partnerCode, BigDecimal amount,
                                           String targetCurrency, String receiverInfo) {
        this(UUID.randomUUID(), Instant.now(), orderId, partnerCode, amount, targetCurrency, receiverInfo);
    }
}
