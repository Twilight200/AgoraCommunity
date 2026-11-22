package com.example.agora.platform.internal;

import com.example.agora.platform.api.FaultPoint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class NoOpFaultPoint implements FaultPoint {

    @Override
    public void hit(String name) {
        // Production deliberately ignores fault injection points.
    }
}