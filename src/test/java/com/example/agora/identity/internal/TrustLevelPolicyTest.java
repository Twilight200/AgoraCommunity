package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrustLevelPolicyTest {

    private final TrustLevelPolicy policy = new TrustLevelPolicy();

    @Test
    void qualifiesWhenTl0ReadTwentyDistinctTopicsAndVisitedThreeDays() {
        assertThat(policy.qualifiesForTl1(0, 20, 3)).isTrue();
    }

    @Test
    void doesNotQualifyWhenTopicReadCountIsBelowThreshold() {
        assertThat(policy.qualifiesForTl1(0, 19, 3)).isFalse();
    }

    @Test
    void doesNotQualifyWhenVisitDaysAreBelowThreshold() {
        assertThat(policy.qualifiesForTl1(0, 20, 2)).isFalse();
    }

    @Test
    void doesNotReapplyTl1PromotionToUserAlreadyAtTl1() {
        assertThat(policy.qualifiesForTl1(1, 20, 3)).isFalse();
    }

    @Test
    void doesNotPromoteHigherTrustLevelBackward() {
        assertThat(policy.qualifiesForTl1(2, 20, 3)).isFalse();
    }
}