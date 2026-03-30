package com.remittance.partner.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.partner.application.PartnerIntegrationService;
import com.remittance.shared.event.IdempotencyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Remittance 모듈의 PartnerProcessingRequestedEvent를 구독하여 파트너사 연동을 수행한다.
 * 멱등성: processed_events 테이블로 eventId 중복 처리를 방지한다.
 */
@Component
public class PartnerEventListener {

    private static final Logger log = LoggerFactory.getLogger(PartnerEventListener.class);
    private static final String SCHEMA = "fintech_partner";

    private final PartnerIntegrationService partnerIntegrationService;
    private final IdempotencyChecker idempotencyChecker;
    private final ObjectMapper objectMapper;

    public PartnerEventListener(PartnerIntegrationService partnerIntegrationService,
                                IdempotencyChecker idempotencyChecker,
                                ObjectMapper objectMapper) {
        this.partnerIntegrationService = partnerIntegrationService;
        this.idempotencyChecker = idempotencyChecker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_remittance", groupId = "partner-remittance-events")
    @Transactional
    public void handleRemittanceEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if (!"PartnerProcessingRequestedEvent".equals(eventType)) return;

            UUID eventId = UUID.fromString(node.get("eventId").asText());
            if (idempotencyChecker.isAlreadyProcessed(SCHEMA, eventId)) {
                log.debug("Duplicate event skipped: eventId={}, type={}", eventId, eventType);
                return;
            }

            UUID orderId = UUID.fromString(node.get("orderId").asText());
            String partnerCode = node.get("partnerCode").asText();
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String targetCurrency = node.get("targetCurrency").asText();
            String receiverInfo = node.has("receiverInfo") ? node.get("receiverInfo").asText() : "";

            log.debug("Partner request: orderId={}, receiver={}", orderId, receiverInfo);
            partnerIntegrationService.processPartnerRequest(orderId, partnerCode, amount, targetCurrency);

            idempotencyChecker.markProcessed(SCHEMA, eventId);
            log.debug("PartnerProcessingRequestedEvent processed: orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to process remittance event for partner: {}", message, e);
            throw new RuntimeException("Partner event processing failed", e);
        }
    }
}
