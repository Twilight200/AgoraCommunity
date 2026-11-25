package com.example.agora.identity.internal;

import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.identity.api.LoginRequest;
import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionTokenService sessionTokenService;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    void encodesPasswordBeforeCreatingAccount() {
        var request = new RegisterRequest(
                "agora_user",
                "correct horse battery staple"
        );
        String passwordHash = "$2a$12$encoded-password-hash";

        when(passwordEncoder.encode(request.password()))
                .thenReturn(passwordHash);

        userAccountService.register(request);

        verify(passwordEncoder).encode(request.password());
        verify(userAccountRepository)
                .createAccount(request.username(), passwordHash);
    }

    @Test
    void rejectsPasswordLongerThanBcryptByteLimit() {
        // 25 个中文字按 UTF-8 编码后是 75 字节。
        var request = new RegisterRequest("agora_user", "中".repeat(25));

        assertThatThrownBy(() -> userAccountService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_PASSWORD_TOO_LONG");
                });

        verifyNoInteractions(passwordEncoder, userAccountRepository);
    }

    @Test
    void translatesDuplicateUsernameToConflict() {
        var request = new RegisterRequest(
                "agora_user",
                "correct horse battery staple"
        );

        when(passwordEncoder.encode(request.password()))
                .thenReturn("$2a$12$encoded-password-hash");

        org.mockito.Mockito.doThrow(
                        new DuplicateKeyException("duplicate username")
                )
                .when(userAccountRepository)
                .createAccount(request.username(), "$2a$12$encoded-password-hash");

        assertThatThrownBy(() -> userAccountService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus())
                            .isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_USERNAME_TAKEN");
                });
    }

    @Test
    void authenticatesAccountWithMatchingPassword() {
        var request = new LoginRequest("agora_user", "correct horse battery staple");
        var credential = new UserAccountCredential(
                42L,
                "agora_user",
                "$2a$12$encoded-password-hash",
                1,
                0
        );

        when(userAccountRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(request.password(), credential.passwordHash()))
                .thenReturn(true);

        long userId = userAccountService.authenticate(request);

        assertThat(userId).isEqualTo(42L);
        verify(passwordEncoder).matches(request.password(), credential.passwordHash());
    }

    @Test
    void checksDummyHashAndReturnsSameUnauthorizedErrorForUnknownAccount() {
        var request = new LoginRequest("missing_user", "some password");

        when(userAccountRepository.findByUsername(request.username()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.matches(
                org.mockito.ArgumentMatchers.eq(request.password()),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        assertThatThrownBy(() -> userAccountService.authenticate(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("IDENTITY_INVALID_CREDENTIALS");
                });

        verify(passwordEncoder).matches(
                org.mockito.ArgumentMatchers.eq(request.password()),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsWrongPasswordWithUnauthorizedError() {
        var request = new LoginRequest("agora_user", "wrong password");
        var credential = new UserAccountCredential(
                42L,
                "agora_user",
                "$2a$12$encoded-password-hash",
                1,
                0
        );

        when(userAccountRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(request.password(), credential.passwordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> userAccountService.authenticate(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus())
                            .isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_INVALID_CREDENTIALS");
                });
    }

    @Test
    void loginPersistsTokenHashAndReturnsRawToken() {
        var request = new LoginRequest("agora_user", "correct horse battery staple");
        var credential = new UserAccountCredential(
                42L,
                "agora_user",
                "$2a$12$encoded-password-hash",
                1,
                0
        );
        var expiresAt = java.time.Instant.parse("2026-11-30T12:00:00Z");
        var issuedToken = new SessionTokenService.IssuedToken(
                "raw-token-value",
                "a".repeat(64),
                expiresAt
        );

        when(userAccountRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(request.password(), credential.passwordHash()))
                .thenReturn(true);
        when(sessionTokenService.issue()).thenReturn(issuedToken);

        LoginResult result = userAccountService.login(request);

        assertThat(result.accessToken()).isEqualTo("raw-token-value");
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        verify(userSessionRepository).create(
                42L,
                "a".repeat(64),
                java.time.LocalDateTime.ofInstant(expiresAt, java.time.ZoneOffset.UTC)
        );
    }

    @Test
    void loginDoesNotIssueOrPersistSessionWhenCredentialsAreInvalid() {
        var request = new LoginRequest("agora_user", "wrong password");
        var credential = new UserAccountCredential(
                42L,
                "agora_user",
                "$2a$12$encoded-password-hash",
                1,
                0
        );

        when(userAccountRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(request.password(), credential.passwordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> userAccountService.login(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("IDENTITY_INVALID_CREDENTIALS");
                });

        verifyNoInteractions(sessionTokenService, userSessionRepository);
    }
}
