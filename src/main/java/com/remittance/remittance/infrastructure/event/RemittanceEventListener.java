package com.remittance.remittance.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.remittance.application.RemittanceService;
import com.remittance.shared.event.IdempotencyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Payment/Partner 모듈의 이벤트를 구독하여 RemittanceOrder 상태를 전이한다.
 * 멱등성: processed_events 테이블로 eventId 중복 처리를 방지한다.
 */
@Component
public class RemittanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(RemittanceEventListener.class);
    private static final String SCHEMA = "fintech_remittance";

    private final RemittanceService remittanceService;
    private final IdempotencyChecker idempotencyChecker;
    private final ObjectMapper objectMapper;

    public RemittanceEventListener(RemittanceService remittanceService,
                                   IdempotencyChecker idempotencyChecker,
                                   ObjectMapper objectMapper) {
        this.remittanceService = remittanceService;
        this.idempotencyChecker = idempotencyChecker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_payment", groupId = "remittance-payment-events")
    @Transactional
    public void handlePaymentEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            if (!eventType.startsWith("Payment")) return;

            UUID eventId = UUID.fromString(node.get("eventId").asText());
            if (idempotencyChecker.isAlreadyProcessed(SCHEMA, eventId)) {
                log.debug("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
                return;
            }

            UUID orderId = UUID.fromString(node.get("orderId").asText());
            switch (eventType) {
                case "PaymentCompletedEvent" -> remittanceService.handlePaymentCompleted(orderId);
                case "PaymentFailedEvent" -> remittanceService.handlePaymentFailed(orderId);
                default -> log.debug("Ignoring payment event: {}", eventType);
            }

            idempotencyChecker.markProcessed(SCHEMA, eventId);
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
            throw new RuntimeException("Payment event processing failed", e);
        }
    }

    @KafkaListener(topics = "outbox.event.fintech_partner", groupId = "remittance-partner-events")
    @Transactional
    public void handlePartnerEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            if (!eventType.startsWith("Partner")) return;

            UUID eventId = UUID.fromString(node.get("eventId").asText());
            if (idempotencyChecker.isAlreadyProcessed(SCHEMA, eventId)) {
                log.debug("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
                return;
            }

            UUID orderId = UUID.fromString(node.get("orderId").asText());
            switch (eventType) {
                case "PartnerCompletedEvent" -> remittanceService.handlePartnerCompleted(orderId);
                case "PartnerFailedEvent" -> remittanceService.handlePartnerFailed(orderId);
                default -> log.debug("Ignoring partner event: {}", eventType);
            }

            idempotencyChecker.markProcessed(SCHEMA, eventId);
        } catch (Exception e) {
            log.error("Failed to process partner event: {}", message, e);
            throw new RuntimeException("Partner event processing failed", e);
        }
    }
}
