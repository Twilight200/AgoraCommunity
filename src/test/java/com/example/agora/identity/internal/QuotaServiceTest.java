package com.example.agora.identity.internal;

import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    private static final long USER_ID = 42L;
    private static final LocalDate UTC_DATE = LocalDate.parse("2026-11-25");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-11-25T00:30:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserDailyQuotaRepository quotaRepository;

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new QuotaService(
                userAccountRepository,
                quotaRepository,
                new QuotaPolicy(),
                FIXED_CLOCK
        );
        when(userAccountRepository.findTrustLevelById(USER_ID))
                .thenReturn(Optional.of(0));
    }

    @Test
    void consumesTopicQuotaAndReturnsRemainingAmount() {
        when(quotaRepository.lockOrCreate(USER_ID, UTC_DATE))
                .thenReturn(new UserDailyQuotaCount(1, 0));

        var decision = quotaService.tryConsumeTopicCreation(USER_ID, false);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(1);
        verify(quotaRepository).increment(QuotaResource.TOPIC, USER_ID, UTC_DATE);
    }

    @Test
    void rejectsLinkForTl0WithoutIncrementingQuota() {
        when(quotaRepository.lockOrCreate(USER_ID, UTC_DATE))
                .thenReturn(new UserDailyQuotaCount(0, 0));

        var decision = quotaService.tryConsumeReplyCreation(USER_ID, true);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode())
                .isEqualTo("IDENTITY_QUOTA_LINK_FORBIDDEN");
        verify(quotaRepository).lockOrCreate(USER_ID, UTC_DATE);
        verifyNoInteractionsAfterLock();
    }

    @Test
    void rejectsWhenDailyQuotaIsExhaustedWithoutIncrementing() {
        when(quotaRepository.lockOrCreate(USER_ID, UTC_DATE))
                .thenReturn(new UserDailyQuotaCount(3, 0));

        var decision = quotaService.tryConsumeTopicCreation(USER_ID, false);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode())
                .isEqualTo("IDENTITY_TOPIC_QUOTA_EXCEEDED");
        verifyNoInteractionsAfterLock();
    }

    @Test
    void rejectsUnknownUserBeforeTouchingQuotaRepository() {
        when(userAccountRepository.findTrustLevelById(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotaService.tryConsumeTopicCreation(USER_ID, false))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("IDENTITY_USER_NOT_FOUND");
                });

        verifyNoInteractions(quotaRepository);
    }

    private void verifyNoInteractionsAfterLock() {
        org.mockito.Mockito.verify(quotaRepository, org.mockito.Mockito.never())
                .increment(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }
}
