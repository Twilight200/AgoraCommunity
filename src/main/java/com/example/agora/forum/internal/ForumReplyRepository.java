package com.example.agora.forum.internal;

import org.springframework.stereotype.Repository;

import java.util.OptionalInt;

@Repository
public class ForumReplyRepository {

    private final ForumReplyMapper mapper;

    public ForumReplyRepository(ForumReplyMapper mapper) {
        this.mapper = mapper;
    }

    public OptionalInt createReply(
            long topicId,
            long replyId,
            long userId,
            String content
    ) {
        int updatedRows = mapper.allocateNextFloor(topicId, userId);

        if (updatedRows != 1) {
            return OptionalInt.empty();
        }

        int floorNo = mapper.findAllocatedFloor();

        int insertedRows = mapper.insertReply(
                replyId,
                topicId,
                floorNo,
                userId,
                content
        );

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Creating a reply must affect exactly one row."
            );
        }

        return OptionalInt.of(floorNo);
    }

    public boolean updateReplyContent(long replyId, long userId, int expectedVersion, String content) {
        return mapper.updateReplyContent(replyId, userId, expectedVersion, content) == 1;
    }

    public boolean softDeleteReply(long replyId, long userId, int expectedVersion) {
        return mapper.softDeleteReply(replyId, userId, expectedVersion) == 1;
    }
}
