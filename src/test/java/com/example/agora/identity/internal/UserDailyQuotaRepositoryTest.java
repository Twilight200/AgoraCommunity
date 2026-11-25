package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDailyQuotaRepositoryTest {

    private static final long USER_ID = 42L;
    private static final LocalDate QUOTA_DATE = LocalDate.parse("2026-11-25");

    @Mock
    private UserDailyQuotaMapper mapper;

    @Test
    void createsQuotaRowThenReadsItForUpdate() {
        var expected = new UserDailyQuotaCount(1, 7);
        when(mapper.findForUpdate(USER_ID, QUOTA_DATE)).thenReturn(expected);

        var repository = new UserDailyQuotaRepository(mapper);
        var actual = repository.lockOrCreate(USER_ID, QUOTA_DATE);

        assertThat(actual).isEqualTo(expected);
        verify(mapper).insertIfAbsent(USER_ID, QUOTA_DATE);
        verify(mapper).findForUpdate(USER_ID, QUOTA_DATE);
    }

    @Test
    void failsIfQuotaRowCannotBeReadAfterCreation() {
        when(mapper.findForUpdate(USER_ID, QUOTA_DATE)).thenReturn(null);

        var repository = new UserDailyQuotaRepository(mapper);

        assertThatThrownBy(() -> repository.lockOrCreate(USER_ID, QUOTA_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Quota row was not found");
    }

    @Test
    void incrementsTheColumnForRequestedResource() {
        when(mapper.incrementTopics(USER_ID, QUOTA_DATE)).thenReturn(1);
        when(mapper.incrementReplies(USER_ID, QUOTA_DATE)).thenReturn(1);
        var repository = new UserDailyQuotaRepository(mapper);

        repository.increment(QuotaResource.TOPIC, USER_ID, QUOTA_DATE);
        repository.increment(QuotaResource.REPLY, USER_ID, QUOTA_DATE);

        verify(mapper).incrementTopics(USER_ID, QUOTA_DATE);
        verify(mapper).incrementReplies(USER_ID, QUOTA_DATE);
    }

    @Test
    void failsIfIncrementDoesNotAffectExactlyOneRow() {
        when(mapper.incrementTopics(USER_ID, QUOTA_DATE)).thenReturn(0);
        var repository = new UserDailyQuotaRepository(mapper);

        assertThatThrownBy(() -> repository.increment(
                QuotaResource.TOPIC,
                USER_ID,
                QUOTA_DATE
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must affect exactly one row");
    }
}
