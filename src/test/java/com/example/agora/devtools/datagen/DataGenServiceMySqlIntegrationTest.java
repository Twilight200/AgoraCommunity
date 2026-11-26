package com.example.agora.devtools.datagen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DataGenServiceMySqlIntegrationTest {

    private static final String MARKER = "datagen_it_";

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.0.36")
                    .withDatabaseName("agora")
                    .withUsername("agora")
                    .withPassword("test-password");

    @DynamicPropertySource
    static void configureDataSource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );
        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );
    }

    @Autowired
    private DataGenService datagenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataGenProperties datagenProperties;

    @AfterEach
    void cleanGeneratedData() {
        jdbcTemplate.update(
                """
                DELETE FROM reply
                WHERE topic_id IN (
                    SELECT id
                    FROM topic
                    WHERE title LIKE 'Datagen Topic %'
                )
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM topic
                WHERE title LIKE 'Datagen Topic %'
                """
        );

        jdbcTemplate.update(
                """
                DELETE FROM category
                WHERE slug LIKE ?
                """,
                MARKER + "%"
        );

        jdbcTemplate.update(
                """
                DELETE FROM user_stat
                WHERE user_id IN (
                    SELECT id
                    FROM user_account
                    WHERE username LIKE ?
                )
                """,
                MARKER + "%"
        );

        jdbcTemplate.update(
                """
                DELETE FROM user_account
                WHERE username LIKE ?
                """,
                MARKER + "%"
        );
    }

    @Test
    void generatesRelatedUsersCategoriesTopicsAndReplies() {
        datagenService.generate();

        Integer userCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_account
                WHERE username LIKE ?
                """,
                Integer.class,
                MARKER + "%"
        );

        Integer userStatCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_stat
                WHERE user_id IN (
                    SELECT id
                    FROM user_account
                    WHERE username LIKE ?
                )
                """,
                Integer.class,
                MARKER + "%"
        );

        Integer categoryCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM category
                WHERE slug LIKE ?
                """,
                Integer.class,
                MARKER + "%"
        );

        Integer topicCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM topic
                WHERE title LIKE 'Datagen Topic %'
                """,
                Integer.class
        );

        Integer replyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM reply
                WHERE content LIKE 'Datagen%'
                   OR content LIKE 'This is the first post%'
                """,
                Integer.class
        );

        assertThat(userCount)
                .isEqualTo(datagenProperties.userCount());

        assertThat(userStatCount)
                .isEqualTo(datagenProperties.userCount());

        assertThat(categoryCount)
                .isEqualTo(datagenProperties.categoryCount());

        assertThat(topicCount)
                .isEqualTo(datagenProperties.topicCount());

        assertThat(replyCount)
                .isEqualTo(
                        datagenProperties.topicCount()
                                * (1 + datagenProperties.repliesPerTopic())
                );
    }

    @Test
    void everyGeneratedTopicHasIndependentFloorSequence() {
        datagenService.generate();

        var topicIds = jdbcTemplate.queryForList(
                """
                SELECT id
                FROM topic
                WHERE title LIKE 'Datagen Topic %'
                ORDER BY id
                """,
                Long.class
        );

        assertThat(topicIds)
                .hasSize(datagenProperties.topicCount());

        for (Long topicId : topicIds) {
            var floors = jdbcTemplate.queryForList(
                    """
                    SELECT floor_no
                    FROM reply
                    WHERE topic_id = ?
                    ORDER BY floor_no
                    """,
                    Integer.class,
                    topicId
            );

            int expectedReplyCount =
                    1 + datagenProperties.repliesPerTopic();

            assertThat(floors)
                    .containsExactly(
                            java.util.stream.IntStream
                                    .rangeClosed(1, expectedReplyCount)
                                    .boxed()
                                    .toArray(Integer[]::new)
                    );

            Integer maxFloor = jdbcTemplate.queryForObject(
                    """
                    SELECT max_floor
                    FROM topic
                    WHERE id = ?
                    """,
                    Integer.class,
                    topicId
            );

            assertThat(maxFloor)
                    .isEqualTo(expectedReplyCount);
        }
    }

    @Test
    void refusesToGenerateTheSameBatchTwice() {
        datagenService.generate();

        assertThatThrownBy(
                () -> datagenService.generate()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Datagen batch already exists"
                );
    }
}