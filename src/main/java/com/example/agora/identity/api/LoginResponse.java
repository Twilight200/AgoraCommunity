package com.example.agora.identity.api;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expireAt
) {
}
