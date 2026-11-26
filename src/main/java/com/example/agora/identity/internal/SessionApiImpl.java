package com.example.agora.identity.internal;

import com.example.agora.identity.api.SessionApi;
import org.springframework.stereotype.Service;

@Service
public class SessionApiImpl implements SessionApi {
    private final SessionService sessionService;

    public SessionApiImpl(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @Override
    public long authenticate(String rawToken){
        return sessionService.authenticate(rawToken);
    }
}
