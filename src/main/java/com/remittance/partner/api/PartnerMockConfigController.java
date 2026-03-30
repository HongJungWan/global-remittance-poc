package com.remittance.partner.api;

import com.remittance.partner.api.dto.MockConfigRequest;
import com.remittance.partner.api.dto.MockConfigResponse;
import com.remittance.partner.application.PartnerMockConfigService;
import com.remittance.partner.application.PartnerMockConfigService.MockConfigSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partner/mock")
public class PartnerMockConfigController {

    private final PartnerMockConfigService mockConfigService;

    public PartnerMockConfigController(PartnerMockConfigService mockConfigService) {
        this.mockConfigService = mockConfigService;
    }

    @PostMapping("/config")
    public ResponseEntity<MockConfigResponse> updateConfig(
            @Valid @RequestBody MockConfigRequest request) {
        MockConfigSnapshot snapshot = mockConfigService.updateConfig(
                request.mode(), request.delayMs(), request.failurePercent());
        return ResponseEntity.ok(new MockConfigResponse(
                snapshot.mode(), snapshot.delayMs(), snapshot.failurePercent()));
    }

    @GetMapping("/config")
    public ResponseEntity<MockConfigResponse> getConfig() {
        MockConfigSnapshot snapshot = mockConfigService.getConfig();
        return ResponseEntity.ok(new MockConfigResponse(
                snapshot.mode(), snapshot.delayMs(), snapshot.failurePercent()));
    }
}
