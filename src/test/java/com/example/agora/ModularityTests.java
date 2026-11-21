package com.example.agora;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules.of(AgoraApplication.class).verify();
    }


    @Test
    void discoversAllApplicationModules() {
        var modules = ApplicationModules.of(AgoraApplication.class);

        assertThat(modules.getModuleByName("identity")).isNotNull();
        assertThat(modules.getModuleByName("forum")).isNotNull();
        assertThat(modules.getModuleByName("reaction")).isNotNull();
        assertThat(modules.getModuleByName("reading")).isNotNull();
        assertThat(modules.getModuleByName("social")).isNotNull();
        assertThat(modules.getModuleByName("feed")).isNotNull();
        assertThat(modules.getModuleByName("ranking")).isNotNull();
        assertThat(modules.getModuleByName("notification")).isNotNull();
        assertThat(modules.getModuleByName("platform")).isNotNull();
    }
}