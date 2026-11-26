package com.example.agora.forum.internal;

import com.example.agora.identity.api.QuotaApi;
import com.example.agora.identity.api.QuotaDecision;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.IdGenerator;
import com.example.agora.platform.api.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumTopicServiceTest {

    private static final long CATEGORY_ID = 10L;
    private static final long USER_ID = 20L;
    private static final long TOPIC_ID = 30L;
    private static final long FIRST_REPLY_ID = 40L;
    private static final String TITLE = "My first topic";
    private static final String CONTENT = "Hello, Agora";

    @Mock
    private ForumTopicRepository topicRepository;

    @Mock
    private QuotaApi quotaApi;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private OutboxPublisher outboxPublisher;

    private ForumTopicService service;

    @BeforeEach
    void setUp() {
        service = new ForumTopicService(
                topicRepository,
                quotaApi,
                idGenerator,
                outboxPublisher
        );
    }

    @Test
    void createsTopicFirstReplyAndOutboxEventWhenQuotaAllows() {
        when(quotaApi.tryConsumeTopicCreation(USER_ID, false))
                .thenReturn(QuotaDecision.allow(2));
        when(idGenerator.nextId()).thenReturn(TOPIC_ID, FIRST_REPLY_ID);

        long createdTopicId = service.createTopic(
                CATEGORY_ID,
                USER_ID,
                TITLE,
                CONTENT
        );

        assertThat(createdTopicId).isEqualTo(TOPIC_ID);

        verify(quotaApi).tryConsumeTopicCreation(USER_ID, false);
        verify(topicRepository).createTopicWithFirstReply(
                TOPIC_ID,
                FIRST_REPLY_ID,
                CATEGORY_ID,
                USER_ID,
                TITLE,
                CONTENT
        );
        verify(outboxPublisher).record(
                "TOPIC",
                TOPIC_ID,
                "TopicCreated",
                Map.of("topicId", TOPIC_ID, "replyId", FIRST_REPLY_ID)
        );
    }

    @Test
    void detectsLinkAndPassesThatFactToQuotaApi() {
        String contentWithLink = "Please read https://example.com/article";

        when(quotaApi.tryConsumeTopicCreation(USER_ID, true))
                .thenReturn(QuotaDecision.allow(2));
        when(idGenerator.nextId()).thenReturn(TOPIC_ID, FIRST_REPLY_ID);

        service.createTopic(
                CATEGORY_ID,
                USER_ID,
                TITLE,
                contentWithLink
        );

        verify(quotaApi).tryConsumeTopicCreation(USER_ID, true);
        verify(topicRepository).createTopicWithFirstReply(
                TOPIC_ID,
                FIRST_REPLY_ID,
                CATEGORY_ID,
                USER_ID,
                TITLE,
                contentWithLink
        );
    }

    @Test
    void returns429AndDoesNotWriteWhenQuotaIsRejected() {
        when(quotaApi.tryConsumeTopicCreation(USER_ID, false))
                .thenReturn(QuotaDecision.deny("IDENTITY_TOPIC_QUOTA_EXCEEDED"));

        assertThatThrownBy(() -> service.createTopic(
                CATEGORY_ID,
                USER_ID,
                TITLE,
                CONTENT
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus().value())
                            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_TOPIC_QUOTA_EXCEEDED");
                });

        verify(quotaApi).tryConsumeTopicCreation(USER_ID, false);
        verifyNoInteractions(idGenerator, topicRepository, outboxPublisher);
    }

    @Test
    void doesNotRecordOutboxEventWhenTopicPersistenceFails() {
        when(quotaApi.tryConsumeTopicCreation(USER_ID, false))
                .thenReturn(QuotaDecision.allow(2));
        when(idGenerator.nextId()).thenReturn(TOPIC_ID, FIRST_REPLY_ID);

        org.mockito.Mockito.doThrow(new IllegalStateException("database error"))
                .when(topicRepository)
                .createTopicWithFirstReply(
                        TOPIC_ID,
                        FIRST_REPLY_ID,
                        CATEGORY_ID,
                        USER_ID,
                        TITLE,
                        CONTENT
                );

        assertThatThrownBy(() -> service.createTopic(
                CATEGORY_ID,
                USER_ID,
                TITLE,
                CONTENT
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database error");

        verifyNoInteractions(outboxPublisher);
    }
}