package com.example.agora.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 18, min = 6)
        @Pattern(regexp = "^[A-Za-z0-9_]+$")
        String username,

        @NotBlank
        @Size(min = 12)
        String password
) {
}
