package com.example.agora.forum.internal;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ForumReplyQueryRepository {
    private final ForumReplyMapper mapper;

    public ForumReplyQueryRepository(ForumReplyMapper mapper){
        this.mapper = mapper;
    }

    public Optional<Long> findTopicAuthorId(long topicId){
        return Optional.ofNullable(mapper.findTopicAuthorId(topicId));
    }

    public List<ReplyView> findNextReplies(
            long topicId,
            int afterFloor,
            int limit
    ) {
        return mapper.findNextReplies(
                topicId,
                afterFloor,
                limit
        );
    }

    public List<ReplyView> findNextAuthorReplies(
            long topicId,
            long userId,
            int afterFloor,
            int limit
    ) {
        return mapper.findNextAuthorReplies(
                topicId,
                userId,
                afterFloor,
                limit
        );
    }
}
