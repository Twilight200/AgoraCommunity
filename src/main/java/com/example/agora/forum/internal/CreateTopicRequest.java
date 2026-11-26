package com.example.agora.forum.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @Positive
        long categoryId,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String content
) {
}
