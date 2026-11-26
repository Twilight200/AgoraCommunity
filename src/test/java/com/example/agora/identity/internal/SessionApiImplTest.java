package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionApiImplTest {

    @Test
    void delegatesAuthenticationToSessionService() {
        SessionService sessionService = mock(SessionService.class);
        SessionApiImpl sessionApi = new SessionApiImpl(sessionService);

        when(sessionService.authenticate("raw-token"))
                .thenReturn(42L);

        long userId = sessionApi.authenticate("raw-token");

        assertThat(userId).isEqualTo(42L);
        verify(sessionService).authenticate("raw-token");
    }
}