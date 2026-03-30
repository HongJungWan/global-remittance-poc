package com.remittance.shared.lock;

/**
 * 획득된 분산 락의 핸들.
 * unlock 시 이 핸들을 전달하여 정확한 락을 해제한다.
 */
public record LockHandle(String key, Object nativeLock) {
}
