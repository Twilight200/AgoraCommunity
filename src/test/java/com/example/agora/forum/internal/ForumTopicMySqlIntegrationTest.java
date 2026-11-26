package com.example.agora.forum.internal;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.identity.internal.UserAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ForumTopicMySqlIntegrationTest {

    @Autowired
    private ForumReplyService replyService;

    @Autowired
    private ForumReplyRepository replyRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private IdGenerator idGenerator;

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
    private ForumTopicService topicService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String username;
    private long userId;
    private long categoryId;
    private Long createdTopicId;

    @BeforeEach
    void createTestUserAndCategory() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        username = "forum_test_" + suffix;
        String slug = "forum-test-" + suffix;

        userAccountService.register(new RegisterRequest(
                username,
                "correct horse battery staple"
        ));

        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE username = ?",
                Long.class,
                username
        );

        jdbcTemplate.update(
                """
                INSERT INTO category (name, slug, sort)
                VALUES (?, ?, 0)
                """,
                "Forum integration test",
                slug
        );

        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE slug = ?",
                Long.class,
                slug
        );
    }

    @AfterEach
    void removeTestData() {
        if (createdTopicId != null) {
            jdbcTemplate.update(
                    "DELETE FROM outbox_event WHERE aggregate_id = ?",
                    createdTopicId
            );
            jdbcTemplate.update(
                    "DELETE FROM reply WHERE topic_id = ?",
                    createdTopicId
            );
            jdbcTemplate.update(
                    "DELETE FROM topic WHERE id = ?",
                    createdTopicId
            );
        }

        jdbcTemplate.update(
                "DELETE FROM category WHERE id = ?",
                categoryId
        );
        jdbcTemplate.update(
                "DELETE FROM user_account WHERE id = ?",
                userId
        );
    }

    @Test
    void createsTopicFirstReplyQuotaAndOutboxEventTogether() {
        createdTopicId = topicService.createTopic(
                categoryId,
                userId,
                "My first topic",
                "This is the first floor."
        );

        var topic = jdbcTemplate.queryForMap(
                """
                SELECT category_id, user_id, title, max_floor
                FROM topic
                WHERE id = ?
                """,
                createdTopicId
        );

        assertThat(((Number) topic.get("category_id")).longValue())
                .isEqualTo(categoryId);
        assertThat(((Number) topic.get("user_id")).longValue())
                .isEqualTo(userId);
        assertThat(topic.get("title")).isEqualTo("My first topic");
        assertThat(((Number) topic.get("max_floor")).intValue()).isEqualTo(1);

        var firstReply = jdbcTemplate.queryForMap(
                """
                SELECT floor_no, user_id, content
                FROM reply
                WHERE topic_id = ?
                """,
                createdTopicId
        );

        assertThat(((Number) firstReply.get("floor_no")).intValue()).isEqualTo(1);
        assertThat(((Number) firstReply.get("user_id")).longValue())
                .isEqualTo(userId);
        assertThat(firstReply.get("content"))
                .isEqualTo("This is the first floor.");

        Integer topicsCreated = jdbcTemplate.queryForObject(
                """
                SELECT topics_created
                FROM user_daily_quota
                WHERE user_id = ?
                  AND quota_date = ?
                """,
                Integer.class,
                userId,
                LocalDate.now(ZoneOffset.UTC)
        );

        assertThat(topicsCreated).isEqualTo(1);

        var outbox = jdbcTemplate.queryForMap(
                """
                SELECT event_type, status,
                       JSON_UNQUOTE(JSON_EXTRACT(payload, '$.topicId')) AS topic_id
                FROM outbox_event
                WHERE aggregate_type = 'TOPIC'
                  AND aggregate_id = ?
                """,
                createdTopicId
        );

        assertThat(outbox.get("event_type")).isEqualTo("TopicCreated");
        assertThat(outbox.get("status")).isEqualTo("NEW");
        assertThat(outbox.get("topic_id").toString())
                .isEqualTo(createdTopicId.toString());
    }

    @Test
    void rollsBackQuotaAndForumWritesWhenTopicInsertFails() {
        Integer outboxCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'TopicCreated'",
                Integer.class
        );

        assertThatThrownBy(() -> topicService.createTopic(
                -1L, // 不存在的板块 ID，触发 category 外键约束
                userId,
                "This should roll back",
                "The topic must not be persisted."
        )).isInstanceOf(RuntimeException.class);

        Integer topicCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topic WHERE user_id = ?",
                Integer.class,
                userId
        );

        Integer replyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reply WHERE user_id = ?",
                Integer.class,
                userId
        );

        Integer quotaRowCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_daily_quota
                WHERE user_id = ?
                  AND quota_date = ?
                """,
                Integer.class,
                userId,
                LocalDate.now(ZoneOffset.UTC)
        );

        Integer outboxCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_type = 'TopicCreated'",
                Integer.class
        );

        assertThat(topicCount).isZero();
        assertThat(replyCount).isZero();
        assertThat(quotaRowCount).isZero();
        assertThat(outboxCountAfter).isEqualTo(outboxCountBefore);
    }

    @Test
    void rollsBackFloorAllocationWhenReplyInsertFails() {
        createdTopicId = topicService.createTopic(
                categoryId,
                userId,
                "Rollback test topic",
                "First floor"
        );

        Long firstReplyId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM reply
                WHERE topic_id = ?
                  AND floor_no = 1
                """,
                Long.class,
                createdTopicId
        );

        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(status ->
                        replyRepository.createReply(
                                createdTopicId,
                                firstReplyId,
                                userId,
                                "This reply should fail"
                        )
                )
        ).isInstanceOf(DataIntegrityViolationException.class);

        Integer maxFloorAfterFailure = jdbcTemplate.queryForObject(
                "SELECT max_floor FROM topic WHERE id = ?",
                Integer.class,
                createdTopicId
        );

        assertThat(maxFloorAfterFailure).isEqualTo(1);

        CreatedReply nextReply = replyService.createReply(
                createdTopicId,
                userId,
                "This should become floor two"
        );

        assertThat(nextReply.floorNo()).isEqualTo(2);

        Integer maxFloorAfterSuccess = jdbcTemplate.queryForObject(
                "SELECT max_floor FROM topic WHERE id = ?",
                Integer.class,
                createdTopicId
        );

        assertThat(maxFloorAfterSuccess).isEqualTo(2);
    }

    @Test
    void rollsBackReplyQuotaAndOutboxWhenTopicCannotAcceptReply() {
        long unavailableTopicId = -999_999L;

        assertThatThrownBy(() ->
                replyService.createReply(
                        unavailableTopicId,
                        userId,
                        "This topic does not exist"
                )
        )
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;

                    assertThat(apiException.getStatus().value())
                            .isEqualTo(409);

                    assertThat(apiException.getCode())
                            .isEqualTo("FORUM_TOPIC_NOT_AVAILABLE");
                });

        Integer quotaRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_daily_quota
                WHERE user_id = ?
                  AND quota_date = ?
                """,
                Integer.class,
                userId,
                LocalDate.now(ZoneOffset.UTC)
        );

        assertThat(quotaRows).isZero();

        Integer outboxRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE aggregate_type = 'TOPIC'
                  AND aggregate_id = ?
                  AND event_type = 'ReplyCreated'
                """,
                Integer.class,
                unavailableTopicId
        );

        assertThat(outboxRows).isZero();
    }

    @Test
    void allocatesUniqueContinuousFloorsFor500ConcurrentReplies()
            throws Exception {

        createdTopicId = topicService.createTopic(
                categoryId,
                userId,
                "Concurrent reply topic",
                "First floor"
        );

        int replyCount = 500;
        int workerCount = 32;

        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);

        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>(replyCount);

        try {
            for (int i = 0; i < replyCount; i++) {
                futures.add(executor.submit(() -> {
                    if (!startSignal.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting for test start."
                        );
                    }

                    long replyId = idGenerator.nextId();

                    return transactionTemplate.execute(status ->
                            replyRepository.createReply(
                                    createdTopicId,
                                    replyId,
                                    userId,
                                    "Concurrent reply"
                            ).orElseThrow()
                    );
                }));
            }

            startSignal.countDown();

            Set<Integer> allocatedFloors = new HashSet<>();

            for (Future<Integer> future : futures) {
                allocatedFloors.add(
                        future.get(120, TimeUnit.SECONDS)
                );
            }

            assertThat(allocatedFloors)
                    .hasSize(replyCount);

            assertThat(allocatedFloors)
                    .contains(2, 501);

            var statistics = jdbcTemplate.queryForMap(
                    """
                    SELECT COUNT(*) AS row_count,
                           COUNT(DISTINCT floor_no) AS distinct_floor_count,
                           MIN(floor_no) AS min_floor,
                           MAX(floor_no) AS max_floor
                    FROM reply
                    WHERE topic_id = ?
                    """,
                    createdTopicId
            );

            // 500 个并发回复 + 原来的第 1 楼
            assertThat(((Number) statistics.get("row_count")).intValue())
                    .isEqualTo(501);

            assertThat(((Number) statistics.get("distinct_floor_count")).intValue())
                    .isEqualTo(501);

            assertThat(((Number) statistics.get("min_floor")).intValue())
                    .isEqualTo(1);

            assertThat(((Number) statistics.get("max_floor")).intValue())
                    .isEqualTo(501);

            Integer maxFloor = jdbcTemplate.queryForObject(
                    "SELECT max_floor FROM topic WHERE id = ?",
                    Integer.class,
                    createdTopicId
            );

            assertThat(maxFloor).isEqualTo(501);
        } finally {
            startSignal.countDown();
            executor.shutdownNow();

            assertThat(executor.awaitTermination(
                    30,
                    TimeUnit.SECONDS
            )).isTrue();
        }
    }



}