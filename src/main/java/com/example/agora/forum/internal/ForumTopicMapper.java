package com.example.agora.forum.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ForumTopicMapper {

    @Insert("""
        INSERT INTO topic (
            id,
            category_id,
            user_id,
            title,
            status,
            pinned,
            max_floor,
            last_reply_at,
            last_reply_user_id,
            version
        )
        VALUES (
            #{topicId},
            #{categoryId},
            #{userId},
            #{title},
            'NORMAL',
            FALSE,
            1,
            CURRENT_TIMESTAMP(6),
            #{userId},
            0
        )
        """)
    int insertTopic(
            @Param("topicId") long topicId,
            @Param("categoryId") long categoryId,
            @Param("userId") long userId,
            @Param("title") String title
    );

    @Insert("""
        INSERT INTO reply (
            id,
            topic_id,
            floor_no,
            user_id,
            content,
            status,
            version
        )
        VALUES (
            #{replyId},
            #{topicId},
            1,
            #{userId},
            #{content},
            'NORMAL',
            0
        )
        """)
    int insertFirstReply(
            @Param("replyId") long replyId,
            @Param("topicId") long topicId,
            @Param("userId") long userId,
            @Param("content") String content
    );
}