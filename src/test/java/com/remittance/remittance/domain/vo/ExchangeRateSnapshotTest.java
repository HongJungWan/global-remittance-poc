package com.remittance.remittance.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateSnapshotTest {

    @Test
    @DisplayName("Lock-in 생성 시 30초 TTL이 설정된다")
    void lock_sets30SecondsTTL() {
        Instant before = Instant.now();
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("1350.00"), "USD", "KRW");
        Instant after = Instant.now();

        assertNotNull(snapshot.lockedAt());
        assertNotNull(snapshot.expiresAt());

        long ttl = snapshot.expiresAt().getEpochSecond() - snapshot.lockedAt().getEpochSecond();
        assertEquals(30, ttl);

        assertTrue(snapshot.lockedAt().compareTo(before) >= 0);
        assertTrue(snapshot.lockedAt().compareTo(after) <= 0);
    }

    @Test
    @DisplayName("만료 전에는 isExpired가 false이다")
    void isExpired_beforeExpiry_returnsFalse() {
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("1350.00"), "USD", "KRW");
        assertFalse(snapshot.isExpired());
    }

    @Test
    @DisplayName("만료 후에는 isExpired가 true이다")
    void isExpired_afterExpiry_returnsTrue() {
        ExchangeRateSnapshot snapshot = new ExchangeRateSnapshot(
                new BigDecimal("1350.00"), "USD", "KRW",
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(30));
        assertTrue(snapshot.isExpired());
    }

    @Test
    @DisplayName("환율 계산: sourceAmount * rate = targetAmount (소수점 2자리 반올림)")
    void calculateTargetAmount_correctCalculation() {
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("1350.12345678"), "USD", "KRW");

        BigDecimal result = snapshot.calculateTargetAmount(new BigDecimal("100.00"));
        // 100 * 1350.12345678 = 135012.345678 → 135012.35
        assertEquals(new BigDecimal("135012.35"), result);
    }

    @Test
    @DisplayName("환율 계산: 소액 정확성 검증")
    void calculateTargetAmount_smallAmount() {
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("0.00074074"), "KRW", "USD");

        BigDecimal result = snapshot.calculateTargetAmount(new BigDecimal("1000000.00"));
        // 1000000 * 0.00074074 = 740.74
        assertEquals(new BigDecimal("740.74"), result);
    }

    @Test
    @DisplayName("통화 정보가 정확히 보존된다")
    void currencyInfo_isPreserved() {
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("56.50"), "USD", "PHP");
        assertEquals("USD", snapshot.sourceCurrency());
        assertEquals("PHP", snapshot.targetCurrency());
    }

    @Test
    @DisplayName("역환율 계산: PHP→USD (1/56.50)")
    void calculateTargetAmount_inverseRate() {
        // PHP → USD 역환율
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("0.01769912"), "PHP", "USD");

        BigDecimal result = snapshot.calculateTargetAmount(new BigDecimal("56500.00"));
        // 56500 * 0.01769912 = 1000.00028 → 1000.00
        assertEquals(new BigDecimal("1000.00"), result);
    }

    @Test
    @DisplayName("역환율 계산: JPY→KRW")
    void calculateTargetAmount_inverseRate_jpyToKrw() {
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.lock(
                new BigDecimal("10.00000000"), "JPY", "KRW");

        BigDecimal result = snapshot.calculateTargetAmount(new BigDecimal("10000.00"));
        // 10000 * 10 = 100000.00
        assertEquals(new BigDecimal("100000.00"), result);
    }
}
