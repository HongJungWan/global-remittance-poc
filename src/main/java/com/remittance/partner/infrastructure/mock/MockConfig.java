package com.remittance.partner.infrastructure.mock;

/**
 * Configurable Mock 설정.
 * 런타임에 POST /api/partner/mock/config로 변경 가능하다.
 */
public class MockConfig {

    public enum Mode {
        SUCCESS, FAILURE, TIMEOUT, RANDOM
    }

    private volatile Mode mode = Mode.SUCCESS;
    private volatile long delayMs = 0;
    private volatile int failurePercent = 30;

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }

    public int getFailurePercent() { return failurePercent; }
    public void setFailurePercent(int failurePercent) { this.failurePercent = failurePercent; }
}
