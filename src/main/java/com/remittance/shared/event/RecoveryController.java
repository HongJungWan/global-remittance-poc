package com.remittance.shared.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 이벤트 복구 관리 API.
 * DLQ 이벤트 재처리, 미처리 Outbox 릴레이, 멱등성 기록 초기화를 제공한다.
 */
@RestController
@RequestMapping("/api/admin/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    /**
     * 미처리 Outbox 이벤트를 Kafka로 수동 릴레이한다.
     */
    @PostMapping("/relay-outbox")
    public ResponseEntity<Map<String, Object>> relayOutboxEvents() {
        int count = recoveryService.relayUnprocessedOutboxEvents();
        return ResponseEntity.ok(Map.of("relayedCount", count));
    }

    /**
     * 특정 이벤트의 멱등성 기록을 초기화하여 재처리를 허용한다.
     */
    @PostMapping("/reset-event/{schema}/{eventId}")
    public ResponseEntity<Map<String, String>> resetProcessedEvent(
            @PathVariable String schema,
            @PathVariable UUID eventId) {
        recoveryService.resetProcessedEvent(schema, eventId);
        return ResponseEntity.ok(Map.of(
                "status", "reset",
                "schema", schema,
                "eventId", eventId.toString()));
    }
}
