package com.remittance.shared.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyCheckerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private IdempotencyChecker idempotencyChecker;

    @Test
    @DisplayName("미처리 이벤트는 isAlreadyProcessed가 false를 반환한다")
    void unprocessedEvent_returnsFalse() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(eventId)))
                .thenReturn(0);

        assertFalse(idempotencyChecker.isAlreadyProcessed("fintech_remittance", eventId));
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 isAlreadyProcessed가 true를 반환한다")
    void processedEvent_returnsTrue() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(eventId)))
                .thenReturn(1);

        assertTrue(idempotencyChecker.isAlreadyProcessed("fintech_remittance", eventId));
    }

    @Test
    @DisplayName("markProcessed 호출 시 INSERT가 실행된다")
    void markProcessed_executesInsert() {
        UUID eventId = UUID.randomUUID();

        idempotencyChecker.markProcessed("fintech_payment", eventId);

        verify(jdbcTemplate).update(contains("fintech_payment"), eq(eventId), any());
    }

    @Test
    @DisplayName("스키마가 null이면 H2용 SQL을 사용한다")
    void nullSchema_usesH2Sql() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(eventId)))
                .thenReturn(0);

        idempotencyChecker.isAlreadyProcessed(null, eventId);

        verify(jdbcTemplate).queryForObject(
                eq("SELECT COUNT(*) FROM processed_events WHERE event_id = ?"),
                eq(Integer.class), eq(eventId));
    }
}
