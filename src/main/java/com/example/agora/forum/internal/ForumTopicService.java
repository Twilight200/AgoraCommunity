package com.example.agora.forum.internal;

import java.util.Map;
import java.util.regex.Pattern;

import com.example.agora.identity.api.QuotaApi;
import com.example.agora.identity.api.QuotaDecision;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.IdGenerator;
import com.example.agora.platform.api.OutboxPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForumTopicService {

    private final ForumTopicRepository topicRepository;
    private final QuotaApi quotaApi;
    private final IdGenerator idGenerator;
    private final OutboxPublisher outboxPublisher;

    public ForumTopicService(ForumTopicRepository topicRepository, QuotaApi quotaApi, IdGenerator idGenerator, OutboxPublisher outboxPublisher){
        this.idGenerator =idGenerator;
        this.outboxPublisher = outboxPublisher;
        this.quotaApi = quotaApi;
        this.topicRepository =topicRepository;
    }

    @Transactional
    public long createTopic(long categoryId, long userId, String title, String content){
        boolean containsLink = ForumLinkDetector.containsLink(content);

        QuotaDecision quotaDecision = quotaApi.tryConsumeTopicCreation(userId,containsLink);

        if(!quotaDecision.allowed()){
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    quotaDecision.reasonCode(),
                    "daily topic creation quota exceeded"
            );
        }

        long topicId = idGenerator.nextId();
        long firstReplyId = idGenerator.nextId();

        topicRepository.createTopicWithFirstReply(
                topicId,
                firstReplyId,
                categoryId,
                userId,
                title,
                content
        );

        outboxPublisher.record("TOPIC", topicId, "TopicCreated", Map.of("topicId", topicId, "replyId", firstReplyId));
        return topicId;
    }

}
