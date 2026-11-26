package com.example.agora.identity.api;

public interface SessionApi {
    long authenticate(String rawToken);
}
