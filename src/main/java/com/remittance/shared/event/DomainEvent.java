package com.remittance.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 기반 인터페이스.
 * Outbox 패턴에서 이벤트 메타데이터를 추출하는 데 사용된다.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
