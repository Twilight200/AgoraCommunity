package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenServiceTest {

    private final SessionTokenService sessionTokenService = new SessionTokenService();

    @Test
    void issuesDifferentUrlSafeTokensFromThirtyTwoRandomBytes() {
        var first = sessionTokenService.issue();
        var second = sessionTokenService.issue();

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(first.rawToken()).doesNotContain("=");
        assertThat(Base64.getUrlDecoder().decode(first.rawToken())).hasSize(32);
    }

    @Test
    void issuedTokenContainsHashOfRawTokenInsteadOfRawTokenAsHash() {
        var issuedToken = sessionTokenService.issue();

        assertThat(issuedToken.tokenHash())
                .isEqualTo(sessionTokenService.hash(issuedToken.rawToken()))
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(issuedToken.rawToken());
    }

    @Test
    void calculatesSha256HashForSuppliedToken() {
        assertThat(sessionTokenService.hash("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void issuedTokenHasAnExpirationTimeInTheFuture() {
        Instant beforeIssue = Instant.now();

        var issuedToken = sessionTokenService.issue();

        assertThat(issuedToken.expiresAt()).isAfter(beforeIssue);
    }
}
