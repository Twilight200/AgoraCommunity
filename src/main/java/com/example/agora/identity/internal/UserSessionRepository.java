package com.example.agora.identity.internal;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserSessionRepository {

    private final UserSessionMapper mapper;

    public UserSessionRepository(UserSessionMapper mapper) {
        this.mapper = mapper;
    }

    public void create(
            long userId,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        int affectedRows = mapper.insert(userId, tokenHash, expiresAt);
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "Creating a session must affect exactly one row."
            );
        }
    }

    public Optional<UserSessionCredential> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash));
    }

    public void revoke(String tokenHash, LocalDateTime revokedAt) {
        mapper.revoke(tokenHash, revokedAt);

    }
}