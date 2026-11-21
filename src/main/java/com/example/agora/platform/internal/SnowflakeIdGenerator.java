package com.example.agora.platform.internal;

import com.example.agora.platform.api.IdGenerator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

public final class SnowflakeIdGenerator implements IdGenerator {

    private static final long CUSTOM_EPOCH_MILLIS =
            Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();

    private static final int WORKER_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_TIMESTAMP = (1L << 41) - 1;

    private static final int TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long MAX_CLOCK_ROLLBACK_MILLIS = 5;
    private static final long MAX_WAIT_MILLIS = 10;

    private final long workerId;
    private final Clock clock;

    private long lastTimestamp = -1;
    private long sequence;

    public SnowflakeIdGenerator(long workerId, Clock clock) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId must be between 0 and " + MAX_WORKER_ID
            );
        }

        this.workerId = workerId;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public synchronized long nextId() {
        long timestamp = currentTimestamp();

        if (timestamp < lastTimestamp) {
            long rollbackMillis = lastTimestamp - timestamp;
            if (rollbackMillis > MAX_CLOCK_ROLLBACK_MILLIS) {
                throw new IllegalStateException(
                        "Clock moved backwards by " + rollbackMillis + " ms"
                );
            }
            timestamp = waitUntilAtLeast(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {
                timestamp = waitUntilAfter(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return (timestamp << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long currentTimestamp() {
        long timestamp = clock.millis() - CUSTOM_EPOCH_MILLIS;

        if (timestamp < 0) {
            throw new IllegalStateException(
                    "Clock is earlier than the configured Snowflake epoch"
            );
        }

        if (timestamp > MAX_TIMESTAMP) {
            throw new IllegalStateException(
                    "Timestamp exceeds the 41-bit Snowflake range"
            );
        }

        return timestamp;
    }

    private long waitUntilAtLeast(long minimumTimestamp) {
        long deadline = System.nanoTime()
                + MAX_WAIT_MILLIS * 1_000_000;

        long timestamp = currentTimestamp();
        while (timestamp < minimumTimestamp) {
            sleepBriefly();

            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException(
                        "Clock did not recover from rollback in time"
                );
            }

            timestamp = currentTimestamp();
        }

        return timestamp;
    }

    private long waitUntilAfter(long previousTimestamp) {
        long deadline = System.nanoTime()
                + MAX_WAIT_MILLIS * 1_000_000;

        long timestamp = currentTimestamp();
        while (timestamp <= previousTimestamp) {
            sleepBriefly();

            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException(
                        "Sequence exhausted before the clock advanced"
                );
            }

            timestamp = currentTimestamp();
        }

        return timestamp;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for the clock",
                    exception
            );
        }
    }
}