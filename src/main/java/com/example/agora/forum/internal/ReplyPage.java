package com.example.agora.forum.internal;

import java.util.List;

public record ReplyPage(
        long topicId,
        int afterFloor,
        Integer nextCursor,
        boolean hasNext,
        boolean onlyAuthor,
        List<ReplyView> replies
) {
}
