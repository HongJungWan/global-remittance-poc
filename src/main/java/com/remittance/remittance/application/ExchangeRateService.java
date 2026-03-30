package com.remittance.remittance.application;

import com.remittance.remittance.domain.vo.ExchangeRateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 환율 캐싱 서비스.
 * Redis에 캐싱된 환율을 조회하고, 캐시 미스 시 Fallback 전략을 적용한다.
 *
 * 캐시 키: fx:rate:{sourceCurrency}:{targetCurrency}
 * 캐시 TTL: 60초
 * 갱신 주기: 10초
 * Lock-in TTL: 30초
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private static final String CACHE_KEY_PREFIX = "fx:rate:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration STALENESS_THRESHOLD = Duration.ofSeconds(60);

    /** PoC용 기본 환율 (외부 Rate Provider 대체) */
    static final Map<String, BigDecimal> DEFAULT_RATES = Map.of(
            "KRW:USD", new BigDecimal("0.00074074"),
            "USD:KRW", new BigDecimal("1350.00000000"),
            "KRW:JPY", new BigDecimal("0.10000000"),
            "JPY:KRW", new BigDecimal("10.00000000"),
            "USD:PHP", new BigDecimal("56.50000000"),
            "PHP:USD", new BigDecimal("0.01769912")
    );

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, CachedRate> lastKnownRates = new ConcurrentHashMap<>();

    public ExchangeRateService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 10초 주기로 외부 Rate Provider에서 환율을 가져와 Redis에 캐싱한다.
     * PoC에서는 DEFAULT_RATES를 외부 API 응답으로 간주한다.
     */
    @Scheduled(fixedRate = 10000)
    public void refreshRates() {
        for (Map.Entry<String, BigDecimal> entry : DEFAULT_RATES.entrySet()) {
            String[] currencies = entry.getKey().split(":");
            String cacheKey = buildCacheKey(currencies[0], currencies[1]);
            cacheRate(cacheKey, entry.getValue());
        }
        log.debug("Exchange rates refreshed: {} pairs cached", DEFAULT_RATES.size());
    }

    /**
     * 환율을 조회하고 Lock-in 스냅샷을 생성한다.
     */
    public ExchangeRateSnapshot getLockedRate(String sourceCurrency, String targetCurrency) {
        BigDecimal rate = getRate(sourceCurrency, targetCurrency);
        return ExchangeRateSnapshot.lock(rate, sourceCurrency, targetCurrency);
    }

    /**
     * 환율 조회 (Redis 캐시 → Fallback → 503).
     */
    public BigDecimal getRate(String sourceCurrency, String targetCurrency) {
        String cacheKey = buildCacheKey(sourceCurrency, targetCurrency);

        // 1차: Redis 캐시 조회
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                BigDecimal rate = new BigDecimal(cached.toString());
                lastKnownRates.put(cacheKey, new CachedRate(rate, System.currentTimeMillis()));
                return rate;
            }
        } catch (Exception e) {
            log.warn("Redis 환율 캐시 조회 실패: key={}", cacheKey, e);
        }

        // 2차: 마지막 유효 환율 Fallback (60초 이내)
        CachedRate lastKnown = lastKnownRates.get(cacheKey);
        if (lastKnown != null) {
            long elapsed = System.currentTimeMillis() - lastKnown.timestamp;
            if (elapsed <= STALENESS_THRESHOLD.toMillis()) {
                log.warn("Stale 환율 사용: key={}, elapsed={}ms", cacheKey, elapsed);
                return lastKnown.rate;
            }
            // 60초 초과: 503 Service Unavailable
            throw new ExchangeRateUnavailableException(
                    "환율이 만료되었습니다 (" + elapsed / 1000 + "초 경과). " +
                            sourceCurrency + " → " + targetCurrency);
        }

        // 3차: PoC 기본 환율 (최초 요청 시 — 스케줄러가 아직 실행되지 않은 경우)
        String pairKey = sourceCurrency + ":" + targetCurrency;
        BigDecimal defaultRate = DEFAULT_RATES.get(pairKey);
        if (defaultRate != null) {
            cacheRate(cacheKey, defaultRate);
            return defaultRate;
        }

        throw new ExchangeRateUnavailableException(
                "환율을 조회할 수 없습니다: " + sourceCurrency + " → " + targetCurrency);
    }

    /**
     * 환율을 Redis에 캐싱한다.
     */
    public void cacheRate(String sourceCurrency, String targetCurrency, BigDecimal rate) {
        String cacheKey = buildCacheKey(sourceCurrency, targetCurrency);
        cacheRate(cacheKey, rate);
    }

    private void cacheRate(String cacheKey, BigDecimal rate) {
        try {
            redisTemplate.opsForValue().set(cacheKey, rate.toPlainString(), CACHE_TTL);
            lastKnownRates.put(cacheKey, new CachedRate(rate, System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("Redis 환율 캐시 저장 실패: key={}", cacheKey, e);
        }
    }

    private String buildCacheKey(String sourceCurrency, String targetCurrency) {
        return CACHE_KEY_PREFIX + sourceCurrency + ":" + targetCurrency;
    }

    private record CachedRate(BigDecimal rate, long timestamp) {
    }

    /**
     * 환율 서비스 불가 예외. 503 Service Unavailable로 매핑된다.
     */
    public static class ExchangeRateUnavailableException extends ResponseStatusException {
        public ExchangeRateUnavailableException(String message) {
            super(HttpStatus.SERVICE_UNAVAILABLE, message);
        }
    }
}
