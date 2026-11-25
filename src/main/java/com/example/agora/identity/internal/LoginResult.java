package com.example.agora.identity.internal;

import java.time.Instant;

public record LoginResult(
        String accessToken,
        Instant expiresAt
) {
}
