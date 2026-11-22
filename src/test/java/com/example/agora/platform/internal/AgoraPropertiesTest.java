package com.example.agora.platform.internal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgoraPropertiesTest {

    @Test
    void bindsApiRole() {
        var properties = bindRole("api");

        assertThat(properties.role()).isEqualTo(AgoraProperties.Role.API);
    }

    @Test
    void bindsWorkerRole() {
        var properties = bindRole("worker");

        assertThat(properties.role()).isEqualTo(AgoraProperties.Role.WORKER);
    }

    @Test
    void rejectsUnknownRole() {
        var binder = new Binder(new MapConfigurationPropertySource(
                Map.of("agora.role", "unknown")
        ));

        assertThatThrownBy(() -> binder.bind("agora", Bindable.of(AgoraProperties.class)).get())
                .isInstanceOf(BindException.class);
    }

    private AgoraProperties bindRole(String role) {
        var binder = new Binder(new MapConfigurationPropertySource(
                Map.of("agora.role", role)
        ));

        return binder.bind("agora", Bindable.of(AgoraProperties.class)).get();
    }
}