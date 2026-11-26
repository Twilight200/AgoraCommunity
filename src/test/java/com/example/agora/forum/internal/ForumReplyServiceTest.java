package com.example.agora.forum.internal;

import com.example.agora.identity.api.QuotaApi;
import com.example.agora.identity.api.QuotaDecision;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.IdGenerator;
import com.example.agora.platform.api.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumReplyServiceTest {

    private static final long TOPIC_ID = 100L;
    private static final long USER_ID = 200L;
    private static final long REPLY_ID = 300L;
    private static final String CONTENT = "A reply";

    @Mock
    private ForumReplyRepository replyRepository;

    @Mock
    private QuotaApi quotaApi;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private OutboxPublisher outboxPublisher;

    private ForumReplyService service;

    @BeforeEach
    void setUp() {
        service = new ForumReplyService(
                replyRepository,
                quotaApi,
                idGenerator,
                outboxPublisher
        );
    }

    @Test
    void createsReplyAndRecordsEventBeforeFloorAllocation() {
        when(quotaApi.tryConsumeReplyCreation(USER_ID, false))
                .thenReturn(QuotaDecision.allow(19));
        when(idGenerator.nextId()).thenReturn(REPLY_ID);
        when(replyRepository.createReply(
                TOPIC_ID, REPLY_ID, USER_ID, CONTENT
        )).thenReturn(OptionalInt.of(2));

        CreatedReply result =
                service.createReply(TOPIC_ID, USER_ID, CONTENT);

        assertThat(result).isEqualTo(new CreatedReply(REPLY_ID, 2));

        InOrder order = inOrder(outboxPublisher, replyRepository);
        order.verify(outboxPublisher).record(
                "TOPIC",
                TOPIC_ID,
                "ReplyCreated",
                Map.of("replyId", REPLY_ID)
        );
        order.verify(replyRepository).createReply(
                TOPIC_ID, REPLY_ID, USER_ID, CONTENT
        );
    }

    @Test
    void detectsLinkBeforeCheckingReplyQuota() {
        String contentWithLink = "Reference: https://example.com/post";

        when(quotaApi.tryConsumeReplyCreation(USER_ID, true))
                .thenReturn(QuotaDecision.allow(19));
        when(idGenerator.nextId()).thenReturn(REPLY_ID);
        when(replyRepository.createReply(
                TOPIC_ID, REPLY_ID, USER_ID, contentWithLink
        )).thenReturn(OptionalInt.of(2));

        service.createReply(TOPIC_ID, USER_ID, contentWithLink);

        verify(quotaApi).tryConsumeReplyCreation(USER_ID, true);
    }

    @Test
    void returns429WithoutWritingWhenQuotaIsRejected() {
        when(quotaApi.tryConsumeReplyCreation(USER_ID, false))
                .thenReturn(QuotaDecision.deny("IDENTITY_REPLY_QUOTA_EXCEEDED"));

        assertThatThrownBy(() ->
                service.createReply(TOPIC_ID, USER_ID, CONTENT)
        )
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus().value())
                            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_REPLY_QUOTA_EXCEEDED");
                });

        verifyNoInteractions(idGenerator, outboxPublisher, replyRepository);
    }

    @Test
    void rollsBackByRejectingUnavailableTopic() {
        when(quotaApi.tryConsumeReplyCreation(USER_ID, false))
                .thenReturn(QuotaDecision.allow(19));
        when(idGenerator.nextId()).thenReturn(REPLY_ID);
        when(replyRepository.createReply(
                TOPIC_ID, REPLY_ID, USER_ID, CONTENT
        )).thenReturn(OptionalInt.empty());

        assertThatThrownBy(() ->
                service.createReply(TOPIC_ID, USER_ID, CONTENT)
        )
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus().value())
                            .isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(apiException.getCode())
                            .isEqualTo("FORUM_TOPIC_NOT_AVAILABLE");
                });

        verify(outboxPublisher).record(
                "TOPIC",
                TOPIC_ID,
                "ReplyCreated",
                Map.of("replyId", REPLY_ID)
        );
    }

    @Test
    void editsReplyWithExpectedVersionAndPublishesEvent() {
        when(replyRepository.updateReplyContent(REPLY_ID, USER_ID, 0, "edited"))
                .thenReturn(true);

        service.editReply(REPLY_ID, USER_ID, 0, "edited");

        verify(outboxPublisher).record(
                "REPLY", REPLY_ID, "ReplyEdited",
                Map.of("replyId", REPLY_ID, "version", 1)
        );
    }

    @Test
    void rejectsEditWhenVersionIsStale() {
        when(replyRepository.updateReplyContent(REPLY_ID, USER_ID, 0, "edited"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.editReply(REPLY_ID, USER_ID, 0, "edited"))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("FORUM_REPLY_VERSION_CONFLICT");
                });

        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void softDeletesReplyAndPublishesEvent() {
        when(replyRepository.softDeleteReply(REPLY_ID, USER_ID, 1))
                .thenReturn(true);

        service.deleteReply(REPLY_ID, USER_ID, 1);

        verify(outboxPublisher).record(
                "REPLY", REPLY_ID, "ReplyDeleted",
                Map.of("replyId", REPLY_ID, "version", 2)
        );
    }
}
