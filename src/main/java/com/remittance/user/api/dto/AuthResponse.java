package com.remittance.user.api.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String accessToken,
        String refreshToken
) {
}
