package com.remittance.shared.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 기반 분산 락 구현체.
 * Watchdog(기본 30초)이 작업 진행 중 락을 자동 연장한다.
 */
@Component
@ConditionalOnBean(RedissonClient.class)
public class RedissonDistributedLockManager implements DistributedLockManager {

    private static final Logger log = LoggerFactory.getLogger(RedissonDistributedLockManager.class);

    private final RedissonClient redissonClient;

    public RedissonDistributedLockManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, long timeout, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            // waitTime=0: 대기 없이 즉시 시도
            // leaseTime=-1: Watchdog 자동 연장 모드 활성화
            boolean acquired = lock.tryLock(0, -1, unit);
            if (acquired) {
                log.debug("Lock acquired: key={}", key);
                return Optional.of(new LockHandle(key, lock));
            }
            log.debug("Lock acquisition failed: key={}", key);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted: key={}", key);
            return Optional.empty();
        }
    }

    @Override
    public void unlock(LockHandle handle) {
        if (handle.nativeLock() instanceof RLock lock && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released: key={}", handle.key());
        }
    }
}
