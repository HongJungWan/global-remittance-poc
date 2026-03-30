package com.remittance.shared.lock;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락 관리 인터페이스.
 * 송금 처리 시 동일 계좌에 대한 동시 요청을 방지한다.
 */
public interface DistributedLockManager {

    /**
     * 락 키 생성 유틸리티.
     * 형식: remittance:account:{accountId}
     */
    static String createAccountLockKey(UUID accountId) {
        return "remittance:account:" + accountId;
    }

    /**
     * 락 획득을 시도한다. 대기 없이 즉시 결과를 반환한다.
     *
     * @param key     락 키
     * @param timeout 락 유지 최대 시간
     * @param unit    시간 단위
     * @return 락 획득 성공 시 LockHandle, 실패 시 empty
     */
    Optional<LockHandle> tryLock(String key, long timeout, TimeUnit unit);

    /**
     * 획득한 락을 해제한다.
     *
     * @param handle tryLock에서 반환된 LockHandle
     */
    void unlock(LockHandle handle);
}
