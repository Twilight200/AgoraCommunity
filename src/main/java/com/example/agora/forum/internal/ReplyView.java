package com.example.agora.forum.internal;

public record ReplyView(long id, int floorNo, long userId, Integer replyToFloor, String content, String status, int version) {
}
