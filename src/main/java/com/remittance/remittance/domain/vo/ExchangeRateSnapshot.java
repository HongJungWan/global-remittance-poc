package com.remittance.remittance.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 환율 확정(Lock-in) 시점의 스냅샷.
 * 환율과 만료 시각을 불변으로 보관한다.
 */
public record ExchangeRateSnapshot(
        BigDecimal rate,
        String sourceCurrency,
        String targetCurrency,
        Instant lockedAt,
        Instant expiresAt
) {
    private static final long LOCK_IN_TTL_SECONDS = 30;

    public static ExchangeRateSnapshot lock(BigDecimal rate, String sourceCurrency, String targetCurrency) {
        Instant now = Instant.now();
        return new ExchangeRateSnapshot(rate, sourceCurrency, targetCurrency, now,
                now.plusSeconds(LOCK_IN_TTL_SECONDS));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public BigDecimal calculateTargetAmount(BigDecimal sourceAmount) {
        return sourceAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
