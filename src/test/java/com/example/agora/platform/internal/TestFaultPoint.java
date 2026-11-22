package com.example.agora.platform.internal;

import com.example.agora.platform.api.FaultPoint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class TestFaultPoint implements FaultPoint {

    private final ConcurrentMap<String, RuntimeException> pendingFailures =
            new ConcurrentHashMap<>();

    void failNext(String name, RuntimeException exception) {
        pendingFailures.put(name, exception);
    }

    @Override
    public void hit(String name) {
        RuntimeException exception = pendingFailures.remove(name);
        if (exception != null) {
            throw exception;
        }
    }
}