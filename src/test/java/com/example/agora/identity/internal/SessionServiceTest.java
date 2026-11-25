package com.example.agora.identity.internal;

import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final String RAW_TOKEN = "raw-access-token";
    private static final String TOKEN_HASH = "a".repeat(64);

    @Mock
    private UserSessionRepository repository;

    @Mock
    private SessionTokenService tokenService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                repository,
                tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    }

    @Test
    void authenticatesActiveUnexpiredSession() {
        when(repository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(
                new UserSessionCredential(
                        1L,
                        42L,
                        TOKEN_HASH,
                        LocalDateTime.ofInstant(NOW.plusSeconds(60), ZoneOffset.UTC),
                        null
                )
        ));

        assertThat(sessionService.authenticate(RAW_TOKEN)).isEqualTo(42L);
        verify(repository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    void rejectsUnknownSession() {
        when(repository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertInvalidSession();
    }

    @Test
    void rejectsExpiredSession() {
        when(repository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(
                new UserSessionCredential(
                        1L,
                        42L,
                        TOKEN_HASH,
                        LocalDateTime.ofInstant(NOW, ZoneOffset.UTC),
                        null
                )
        ));

        assertInvalidSession();
    }

    @Test
    void rejectsRevokedSession() {
        when(repository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(
                new UserSessionCredential(
                        1L,
                        42L,
                        TOKEN_HASH,
                        LocalDateTime.ofInstant(NOW.plusSeconds(60), ZoneOffset.UTC),
                        LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC)
                )
        ));

        assertInvalidSession();
    }

    @Test
    void revokesSessionUsingHashAndCurrentUtcTime() {
        sessionService.revoke(RAW_TOKEN);

        verify(repository).revoke(
                TOKEN_HASH,
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }

    private void assertInvalidSession() {
        assertThatThrownBy(() -> sessionService.authenticate(RAW_TOKEN))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("IDENTITY_INVALID_SESSION");
                });
    }
}
