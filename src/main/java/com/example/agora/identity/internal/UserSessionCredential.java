package com.example.agora.identity.internal;

import java.time.LocalDateTime;

public record UserSessionCredential(
        long id,
        long userId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
) {
}
