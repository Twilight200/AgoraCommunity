package com.example.agora.platform.internal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void generatesUniqueAndIncreasingIds() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(generator.nextId());
        }

        assertThat(ids).doesNotHaveDuplicates();

        for (int i = 1; i < ids.size(); i++) {
            assertThat(ids.get(i)).isGreaterThan(ids.get(i - 1));
        }
    }

    @Test
    void rejectsWorkerIdOutsideSupportedRange() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, clock))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTimeEarlierThanCustomEpoch() {
        Clock clock = Clock.fixed(
                Instant.parse("2023-12-31T23:59:59Z"),
                ZoneOffset.UTC
        );
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("earlier");
    }

    @Test
    void differentWorkerIdsProduceDifferentIdsAtSameTime() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        SnowflakeIdGenerator first = new SnowflakeIdGenerator(1, clock);
        SnowflakeIdGenerator second = new SnowflakeIdGenerator(2, clock);

        assertThat(first.nextId()).isNotEqualTo(second.nextId());
    }
}