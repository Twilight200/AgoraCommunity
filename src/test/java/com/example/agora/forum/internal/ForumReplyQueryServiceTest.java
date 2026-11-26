package com.example.agora.forum.internal;

import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumReplyQueryServiceTest {

    private static final long TOPIC_ID = 100L;
    private static final long AUTHOR_ID = 200L;

    @Mock
    private ForumReplyQueryRepository repository;

    private ForumReplyQueryService service;

    @BeforeEach
    void setUp() {
        service = new ForumReplyQueryService(repository);
    }

    @Test
    void readsAuthorRepliesByCursorAcrossNonAuthorFloors() {
        List<ReplyView> replies = List.of(
                reply(1, AUTHOR_ID),
                reply(3, AUTHOR_ID),
                reply(8, AUTHOR_ID),
                reply(15, AUTHOR_ID),
                reply(22, AUTHOR_ID),
                reply(30, AUTHOR_ID)
        );

        when(repository.findTopicAuthorId(TOPIC_ID))
                .thenReturn(Optional.of(AUTHOR_ID));

        when(repository.findNextAuthorReplies(
                TOPIC_ID,
                AUTHOR_ID,
                0,
                21
        )).thenReturn(replies);

        ReplyPage result =
                service.readAuthorReplies(TOPIC_ID, null);

        assertThat(result.replies())
                .extracting(ReplyView::floorNo)
                .containsExactly(
                        1,
                        3,
                        8,
                        15,
                        22,
                        30
                );

        assertThat(result.hasNext())
                .isFalse();

        assertThat(result.nextCursor())
                .isNull();
    }

    @Test
    void continuesAuthorRepliesFromNextCursor() {
        when(repository.findTopicAuthorId(TOPIC_ID))
                .thenReturn(Optional.of(AUTHOR_ID));

        List<ReplyView> replies =
                IntStream.rangeClosed(21, 40)
                        .mapToObj(floor ->
                                reply(floor, AUTHOR_ID)
                        )
                        .toList();

        when(repository.findNextAuthorReplies(
                TOPIC_ID,
                AUTHOR_ID,
                20,
                21
        )).thenReturn(replies);

        ReplyPage result =
                service.readAuthorReplies(
                        TOPIC_ID,
                        20
                );

        assertThat(result.replies())
                .hasSize(20);

        assertThat(result.replies().get(0).floorNo())
                .isEqualTo(21);

        assertThat(result.replies().get(19).floorNo())
                .isEqualTo(40);

        assertThat(result.hasNext())
                .isFalse();

        assertThat(result.nextCursor())
                .isNull();
    }

    @Test
    void deletedFloorsAreAlreadyExcludedByRepositoryQuery() {
        when(repository.findTopicAuthorId(TOPIC_ID))
                .thenReturn(Optional.of(AUTHOR_ID));

        when(repository.findNextReplies(
                TOPIC_ID,
                0,
                21
        )).thenReturn(List.of(
                reply(1, AUTHOR_ID),
                reply(3, AUTHOR_ID),
                reply(5, AUTHOR_ID)
        ));

        ReplyPage result =
                service.readReplies(TOPIC_ID, null);

        assertThat(result.replies())
                .extracting(ReplyView::floorNo)
                .containsExactly(1, 3, 5);
    }

    @Test
    void returnsAtMostTwentyRepliesAndUsesTwentiethFloorAsNextCursor() {
        List<ReplyView> fetched = IntStream.rangeClosed(1, 21)
                .mapToObj(floor -> reply(floor, AUTHOR_ID))
                .toList();

        when(repository.findTopicAuthorId(TOPIC_ID))
                .thenReturn(Optional.of(AUTHOR_ID));
        when(repository.findNextReplies(TOPIC_ID, 0, 21))
                .thenReturn(fetched);

        ReplyPage result = service.readReplies(TOPIC_ID, null);

        assertThat(result.replies()).hasSize(20);
        assertThat(result.replies().get(19).floorNo()).isEqualTo(20);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(20);
    }

    @Test
    void rejectsMissingTopicBeforeReadingReplies() {
        when(repository.findTopicAuthorId(TOPIC_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.readReplies(TOPIC_ID, null))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode())
                            .isEqualTo("FORUM_TOPIC_NOT_FOUND");
                });

        verify(repository).findTopicAuthorId(TOPIC_ID);
        verifyNoMoreInteractions(repository);
    }

    private ReplyView reply(int floorNo, long userId) {
        return new ReplyView(
                floorNo + 1000L,
                floorNo,
                userId,
                null,
                "reply-" + floorNo,
                "NORMAL",
                0
        );
    }
}