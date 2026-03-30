package com.remittance.shared.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DistributedLockManagerTest {

    @Test
    @DisplayName("계좌 락 키가 올바른 형식으로 생성된다")
    void createAccountLockKey_hasCorrectFormat() {
        UUID accountId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String key = DistributedLockManager.createAccountLockKey(accountId);

        assertEquals("remittance:account:550e8400-e29b-41d4-a716-446655440000", key);
    }

    @Test
    @DisplayName("서로 다른 계좌는 서로 다른 락 키를 가진다")
    void differentAccounts_haveDifferentKeys() {
        UUID account1 = UUID.randomUUID();
        UUID account2 = UUID.randomUUID();

        String key1 = DistributedLockManager.createAccountLockKey(account1);
        String key2 = DistributedLockManager.createAccountLockKey(account2);

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("동일 계좌는 동일한 락 키를 생성한다")
    void sameAccount_generatesSameKey() {
        UUID accountId = UUID.randomUUID();

        String key1 = DistributedLockManager.createAccountLockKey(accountId);
        String key2 = DistributedLockManager.createAccountLockKey(accountId);

        assertEquals(key1, key2);
    }

    @Test
    @DisplayName("NoOp 구현체는 항상 락 획득에 성공한다")
    void noOpLockManager_alwaysAcquires() {
        NoOpDistributedLockManager lockManager = new NoOpDistributedLockManager();
        String key = DistributedLockManager.createAccountLockKey(UUID.randomUUID());

        Optional<LockHandle> handle = lockManager.tryLock(key, 10, TimeUnit.SECONDS);

        assertTrue(handle.isPresent());
        assertEquals(key, handle.get().key());
    }

    @Test
    @DisplayName("NoOp 구현체의 unlock은 예외 없이 수행된다")
    void noOpLockManager_unlockDoesNotThrow() {
        NoOpDistributedLockManager lockManager = new NoOpDistributedLockManager();
        LockHandle handle = new LockHandle("test-key", "noop");

        assertDoesNotThrow(() -> lockManager.unlock(handle));
    }
}
