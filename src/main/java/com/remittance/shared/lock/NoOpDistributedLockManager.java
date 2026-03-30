package com.remittance.shared.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 테스트 환경용 NoOp 분산 락 구현체.
 * RedissonClient 빈이 없을 때 자동으로 활성화된다.
 * 항상 락 획득에 성공한다.
 */
@Component
@ConditionalOnMissingBean(RedissonDistributedLockManager.class)
public class NoOpDistributedLockManager implements DistributedLockManager {

    private static final Logger log = LoggerFactory.getLogger(NoOpDistributedLockManager.class);

    @Override
    public Optional<LockHandle> tryLock(String key, long timeout, TimeUnit unit) {
        log.debug("NoOp lock acquired: key={}", key);
        return Optional.of(new LockHandle(key, "noop"));
    }

    @Override
    public void unlock(LockHandle handle) {
        log.debug("NoOp lock released: key={}", handle.key());
    }
}
