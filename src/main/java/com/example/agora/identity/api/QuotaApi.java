package com.example.agora.identity.api;

public interface QuotaApi {

    QuotaDecision tryConsumeTopicCreation(
            long userId,
            boolean containsLink
    );

    QuotaDecision tryConsumeReplyCreation(
            long userId,
            boolean containsLink
    );
}