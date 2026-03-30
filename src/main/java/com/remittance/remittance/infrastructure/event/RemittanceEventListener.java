package com.remittance.remittance.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.remittance.application.RemittanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Payment/Partner 모듈의 이벤트를 구독하여 RemittanceOrder 상태를 전이한다.
 */
@Component
public class RemittanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(RemittanceEventListener.class);

    private final RemittanceService remittanceService;
    private final ObjectMapper objectMapper;

    public RemittanceEventListener(RemittanceService remittanceService, ObjectMapper objectMapper) {
        this.remittanceService = remittanceService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_payment", groupId = "remittance-payment-events")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            UUID orderId = UUID.fromString(node.get("orderId").asText());

            switch (eventType) {
                case "PaymentCompletedEvent" -> remittanceService.handlePaymentCompleted(orderId);
                case "PaymentFailedEvent" -> remittanceService.handlePaymentFailed(orderId);
                default -> log.debug("Ignoring payment event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
        }
    }

    @KafkaListener(topics = "outbox.event.fintech_partner", groupId = "remittance-partner-events")
    public void handlePartnerEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
            UUID orderId = UUID.fromString(node.get("orderId").asText());

            switch (eventType) {
                case "PartnerCompletedEvent" -> remittanceService.handlePartnerCompleted(orderId);
                case "PartnerFailedEvent" -> remittanceService.handlePartnerFailed(orderId);
                default -> log.debug("Ignoring partner event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process partner event: {}", message, e);
        }
    }
}
