package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuotaPolicyTest {

    private final QuotaPolicy quotaPolicy = new QuotaPolicy();

    @Test
    void tl0CanCreateTopicWhenBelowDailyLimit() {
        var decision = quotaPolicy.decide(
                0,
                QuotaResource.TOPIC,
                0,
                false
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(2);
        assertThat(decision.reasonCode()).isNull();
    }

    @Test
    void tl0CannotCreateFourthTopic() {
        var decision = quotaPolicy.decide(
                0,
                QuotaResource.TOPIC,
                3,
                false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode())
                .isEqualTo("IDENTITY_TOPIC_QUOTA_EXCEEDED");
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void tl0CannotCreateReplyContainingLink() {
        var decision = quotaPolicy.decide(
                0,
                QuotaResource.REPLY,
                0,
                true
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode())
                .isEqualTo("IDENTITY_QUOTA_LINK_FORBIDDEN");
    }

    @Test
    void tl0CanCreateReplyWithoutLink() {
        var decision = quotaPolicy.decide(
                0,
                QuotaResource.REPLY,
                19,
                false
        );

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(0);
    }

    @Test
    void tl1HasHigherTopicAndReplyLimits() {
        var topicDecision = quotaPolicy.decide(
                1,
                QuotaResource.TOPIC,
                9,
                false
        );

        var replyDecision = quotaPolicy.decide(
                1,
                QuotaResource.REPLY,
                99,
                true
        );

        assertThat(topicDecision.allowed()).isTrue();
        assertThat(topicDecision.remaining()).isZero();

        assertThat(replyDecision.allowed()).isTrue();
        assertThat(replyDecision.remaining()).isZero();
    }

    @Test
    void rejectsUnsupportedTrustLevel() {
        assertThatThrownBy(() -> quotaPolicy.decide(
                2,
                QuotaResource.TOPIC,
                0,
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported trust level");
    }
}