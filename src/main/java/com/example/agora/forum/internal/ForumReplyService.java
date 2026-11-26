package com.example.agora.forum.internal;

import com.example.agora.identity.api.QuotaApi;
import com.example.agora.identity.api.QuotaDecision;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.IdGenerator;
import com.example.agora.platform.api.OutboxPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.OptionalInt;

@Service
public class ForumReplyService {
    private final ForumReplyRepository replyRepository;
    private final QuotaApi quotaApi;
    private final IdGenerator idGenerator;
    private final OutboxPublisher outboxPublisher;

    public ForumReplyService(
            ForumReplyRepository replyRepository,
            QuotaApi quotaApi,
            IdGenerator idGenerator,
            OutboxPublisher outboxPublisher
    ) {
        this.replyRepository = replyRepository;
        this.quotaApi = quotaApi;
        this.idGenerator = idGenerator;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public CreatedReply createReply(long topicId, long userId, String content){
        boolean containsLink = ForumLinkDetector.containsLink(content);

        QuotaDecision decision = quotaApi.tryConsumeReplyCreation(userId,containsLink);

        if(!decision.allowed()){
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    decision.reasonCode(),
                    "daily reply creation quota exceeded"
            );
        }

        long replyId = idGenerator.nextId();

        outboxPublisher.record(
                "TOPIC",
                topicId,
                "ReplyCreated",
                Map.of("replyId", replyId)
        );

        OptionalInt floor = replyRepository.createReply(topicId,replyId, userId, content);
        if(floor.isEmpty()){
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "FORUM_TOPIC_NOT_AVAILABLE",
                    "topic does not accept replies"
            );
        }

        return new CreatedReply(replyId,floor.getAsInt());
    }

    @Transactional
    public void editReply(long replyId, long userId, int expectedVersion, String content) {
        if (!replyRepository.updateReplyContent(replyId, userId, expectedVersion, content)) {
            throw editConflict();
        }
        outboxPublisher.record(
                "REPLY", replyId, "ReplyEdited",
                Map.of("replyId", replyId, "version", expectedVersion + 1)
        );
    }

    @Transactional
    public void deleteReply(long replyId, long userId, int expectedVersion) {
        if (!replyRepository.softDeleteReply(replyId, userId, expectedVersion)) {
            throw editConflict();
        }
        outboxPublisher.record(
                "REPLY", replyId, "ReplyDeleted",
                Map.of("replyId", replyId, "version", expectedVersion + 1)
        );
    }

    private ApiException editConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "FORUM_REPLY_VERSION_CONFLICT",
                "reply was changed, deleted, or does not belong to the user"
        );
    }
}
