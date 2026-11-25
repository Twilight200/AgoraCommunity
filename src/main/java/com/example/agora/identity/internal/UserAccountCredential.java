package com.example.agora.identity.internal;

public record UserAccountCredential(long id, String username, String passwordHash, int status, int trustLevel) {
}
