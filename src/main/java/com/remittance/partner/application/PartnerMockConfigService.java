package com.remittance.partner.application;

import com.remittance.partner.infrastructure.mock.ConfigurableMockPartnerClient;
import com.remittance.partner.infrastructure.mock.MockConfig;
import org.springframework.stereotype.Service;

@Service
public class PartnerMockConfigService {

    private final ConfigurableMockPartnerClient partnerClient;

    public PartnerMockConfigService(ConfigurableMockPartnerClient partnerClient) {
        this.partnerClient = partnerClient;
    }

    public MockConfigSnapshot updateConfig(String mode, long delayMs, int failurePercent) {
        MockConfig config = partnerClient.getConfig();
        config.setMode(MockConfig.Mode.valueOf(mode));
        config.setDelayMs(delayMs);
        config.setFailurePercent(failurePercent);
        return toSnapshot(config);
    }

    public MockConfigSnapshot getConfig() {
        return toSnapshot(partnerClient.getConfig());
    }

    private MockConfigSnapshot toSnapshot(MockConfig config) {
        return new MockConfigSnapshot(config.getMode().name(), config.getDelayMs(), config.getFailurePercent());
    }

    public record MockConfigSnapshot(String mode, long delayMs, int failurePercent) {
    }
}
