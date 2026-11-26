package com.example.agora.forum.internal;

import jakarta.validation.constraints.NotBlank;

public record CreateReplyRequest(
        @NotBlank
        String content
) {
}
