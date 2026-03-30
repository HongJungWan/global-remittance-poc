package com.remittance.partner.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remittance.partner.application.PartnerIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Remittance 모듈의 PartnerProcessingRequestedEvent를 구독하여 파트너사 연동을 수행한다.
 */
@Component
public class PartnerEventListener {

    private static final Logger log = LoggerFactory.getLogger(PartnerEventListener.class);

    private final PartnerIntegrationService partnerIntegrationService;
    private final ObjectMapper objectMapper;

    public PartnerEventListener(PartnerIntegrationService partnerIntegrationService,
                                ObjectMapper objectMapper) {
        this.partnerIntegrationService = partnerIntegrationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "outbox.event.fintech_remittance", groupId = "partner-remittance-events")
    public void handleRemittanceEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if ("PartnerProcessingRequestedEvent".equals(eventType)) {
                UUID orderId = UUID.fromString(node.get("orderId").asText());
                String partnerCode = node.get("partnerCode").asText();
                BigDecimal amount = new BigDecimal(node.get("amount").asText());
                String targetCurrency = node.get("targetCurrency").asText();

                partnerIntegrationService.processPartnerRequest(
                        orderId, partnerCode, amount, targetCurrency);
                log.debug("PartnerProcessingRequestedEvent processed: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process remittance event for partner: {}", message, e);
        }
    }
}
