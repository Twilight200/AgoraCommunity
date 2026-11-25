package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(PasswordEncoderConfiguration.class)
class PasswordEncoderConfigurationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void encodesPasswordWithSaltAndCanVerifyIt() {
        String rawPassword = "correct horse battery staple";

        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        assertThat(firstHash).isNotEqualTo(rawPassword);
        assertThat(secondHash).isNotEqualTo(rawPassword);
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(passwordEncoder.matches(rawPassword, firstHash)).isTrue();
    }
}