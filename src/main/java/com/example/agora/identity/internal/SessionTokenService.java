package com.example.agora.identity.internal;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SessionTokenService {
    private static final Duration TOKEN_LIFETIME = Duration.ofDays(7);

    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedToken issue(){
        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiresAt = Instant.now().plus(TOKEN_LIFETIME);

        return new IssuedToken(rawToken, sha256(rawToken),expiresAt);
    }

    public String hash(String rawToken){
        return sha256(rawToken);
    }

    private String sha256(String token){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record IssuedToken(String rawToken, String tokenHash, Instant expiresAt){}
}

