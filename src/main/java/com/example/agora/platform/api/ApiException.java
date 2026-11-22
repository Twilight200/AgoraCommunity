package com.example.agora.platform.api;

import org.springframework.http.HttpStatusCode;

import java.util.Objects;

public class ApiException extends RuntimeException{
    private final HttpStatusCode status;
    private final String code;

    public ApiException(HttpStatusCode status, String code, String detail) {
        super(detail);
        this.status = Objects.requireNonNull(status, "status must not be null");

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = code;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}


