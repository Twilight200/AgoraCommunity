package com.example.agora.platform.internal;

import com.example.agora.platform.api.OutboxPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OutboxPublisherMySqlIntegrationTest {

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.0.36")
                    .withDatabaseName("agora")
                    .withUsername("agora")
                    .withPassword("test-password");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String createdEventId;

    @AfterEach
    void removeTestEvent() {
        if (createdEventId != null) {
            jdbcTemplate.update(
                    "DELETE FROM outbox_event WHERE event_id = ?",
                    createdEventId
            );
        }
    }

    @Test
    void writesNewEventWithJsonPayload() {
        createdEventId = outboxPublisher.record(
                "TOPIC",
                1001L,
                "TopicCreated",
                Map.of("topicId", 1001L, "replyId", 2001L)
        );

        var event = jdbcTemplate.queryForMap(
                """
                
                        SELECT aggregate_type, aggregate_id, event_type,
                       payload, status, attempts
                FROM outbox_event
                WHERE event_id = ?
                """,
                createdEventId
        );
        String topicId = jdbcTemplate.queryForObject(
                        """
                SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.topicId'))
                FROM outbox_event
                WHERE event_id = ?
                """,
                String.class,
                createdEventId
        );

        String replyId = jdbcTemplate.queryForObject(
                        """
                SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.replyId'))
                FROM outbox_event
                WHERE event_id = ?
                """,
                String.class,
                createdEventId
        );

        assertThat(topicId).isEqualTo("1001");
        assertThat(replyId).isEqualTo("2001");
        assertThat(event.get("aggregate_type")).isEqualTo("TOPIC");
        assertThat(((Number) event.get("aggregate_id")).longValue())
                .isEqualTo(1001L);
        assertThat(event.get("event_type")).isEqualTo("TopicCreated");
        assertThat(event.get("status")).isEqualTo("NEW");
        assertThat(((Number) event.get("attempts")).intValue()).isZero();
        }

    @Test
    void rollsBackOutboxEventWithCallingTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            createdEventId = outboxPublisher.record(
                    "TOPIC",
                    1002L,
                    "TopicCreated",
                    Map.of("topicId", 1002L, "replyId", 2002L)
            );

            status.setRollbackOnly();
        });

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_id = ?",
                Integer.class,
                createdEventId
        );

        assertThat(count).isZero();
    }
}