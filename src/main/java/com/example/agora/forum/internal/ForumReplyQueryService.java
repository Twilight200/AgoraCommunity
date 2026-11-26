package com.example.agora.forum.internal;

import com.example.agora.forum.internal.ForumReplyQueryRepository;
import com.example.agora.platform.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumReplyQueryService {

    public static final int PAGE_SIZE = 20;
    private static final int PROBE_SIZE =
            PAGE_SIZE + 1;

    private final ForumReplyQueryRepository repository;

    public ForumReplyQueryService(
            ForumReplyQueryRepository repository
    ) {
        this.repository = repository;
    }

    public ReplyPage readReplies(
            long topicId,
            Integer afterFloor
    ) {
        int cursor = normalizeCursor(afterFloor);

        requireTopicAuthorId(topicId);

        List<ReplyView> fetched =
                repository.findNextReplies(
                        topicId,
                        cursor,
                        PROBE_SIZE
                );

        return buildPage(
                topicId,
                cursor,
                false,
                fetched
        );
    }

    public ReplyPage readAuthorReplies(
            long topicId,
            Integer afterFloor
    ) {
        int cursor = normalizeCursor(afterFloor);

        long authorId = requireTopicAuthorId(topicId);

        List<ReplyView> fetched =
                repository.findNextAuthorReplies(
                        topicId,
                        authorId,
                        cursor,
                        PROBE_SIZE
                );

        return buildPage(
                topicId,
                cursor,
                true,
                fetched
        );
    }

    private ReplyPage buildPage(
            long topicId,
            int cursor,
            boolean onlyAuthor,
            List<ReplyView> fetched
    ) {
        boolean hasNext =
                fetched.size() > PAGE_SIZE;

        List<ReplyView> visibleReplies =
                hasNext
                        ? fetched.subList(0, PAGE_SIZE)
                        : fetched;

        Integer nextCursor = null;

        if (hasNext) {
            nextCursor =
                    visibleReplies
                            .get(visibleReplies.size() - 1)
                            .floorNo();
        }

        return new ReplyPage(
                topicId,
                cursor,
                nextCursor,
                hasNext,
                onlyAuthor,
                List.copyOf(visibleReplies)
        );
    }

    private int normalizeCursor(Integer afterFloor) {
        int cursor = afterFloor == null
                ? 0
                : afterFloor;

        if (cursor < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "FORUM_INVALID_CURSOR",
                    "cursor must not be negative"
            );
        }

        return cursor;
    }

    private long requireTopicAuthorId(long topicId){
        return repository.findTopicAuthorId(topicId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "FORUM_TOPIC_NOT_FOUND",
                        "topic does not exist"
                ));
    }
}