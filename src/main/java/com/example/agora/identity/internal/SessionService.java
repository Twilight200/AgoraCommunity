package com.example.agora.identity.internal;

import com.example.agora.platform.api.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SessionService {

    private final UserSessionRepository repository;
    private final SessionTokenService tokenService;
    private final Clock clock;

    @Autowired
    public SessionService(
            UserSessionRepository repository,
            SessionTokenService tokenService
    ) {
        this(repository, tokenService, Clock.systemUTC());
    }

    SessionService(
            UserSessionRepository repository,
            SessionTokenService tokenService,
            Clock clock
    ) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public long authenticate(String rawToken) {
        String tokenHash = tokenService.hash(rawToken);

        var session = repository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        LocalDateTime now = LocalDateTime.ofInstant(
                clock.instant(),
                ZoneOffset.UTC
        );

        if (session.revokedAt() != null || !session.expiresAt().isAfter(now)) {
            throw invalidToken();
        }

        return session.userId();
    }

    public void revoke(String rawToken) {
        String tokenHash = tokenService.hash(rawToken);
        LocalDateTime now = LocalDateTime.ofInstant(
                clock.instant(),
                ZoneOffset.UTC
        );
        repository.revoke(tokenHash, now);
    }

    private ApiException invalidToken() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "IDENTITY_INVALID_SESSION",
                "session is invalid"
        );
    }
}