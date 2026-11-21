package com.example.agora.platform.internal;

import com.example.agora.platform.api.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdGeneratorConfigurationTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void registersIdGeneratorBean() {
        long first = idGenerator.nextId();
        long second = idGenerator.nextId();

        assertThat(second).isGreaterThan(first);
    }
}