package com.example.agora.forum.internal;

import org.springframework.stereotype.Repository;

@Repository
public class ForumTopicRepository {

    private final ForumTopicMapper mapper;

    public ForumTopicRepository(ForumTopicMapper mapper) {
        this.mapper = mapper;
    }

    public void createTopicWithFirstReply(
            long topicId,
            long firstReplyId,
            long categoryId,
            long userId,
            String title,
            String content
    ) {
        int topicRows = mapper.insertTopic(
                topicId,
                categoryId,
                userId,
                title
        );

        if (topicRows != 1) {
            throw new IllegalStateException(
                    "Creating a topic must affect exactly one row."
            );
        }

        int replyRows = mapper.insertFirstReply(
                firstReplyId,
                topicId,
                userId,
                content
        );

        if (replyRows != 1) {
            throw new IllegalStateException(
                    "Creating the first reply must affect exactly one row."
            );
        }
    }


}