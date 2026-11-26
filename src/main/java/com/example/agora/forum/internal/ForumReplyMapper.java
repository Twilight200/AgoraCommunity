package com.example.agora.forum.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ForumReplyMapper {

    @Update("""
        UPDATE topic
        SET max_floor = LAST_INSERT_ID(max_floor + 1),
            last_reply_at = CURRENT_TIMESTAMP(6),
            last_reply_user_id = #{userId},
            version = version + 1
        WHERE id = #{topicId}
          AND status = 'NORMAL'
        """)
    int allocateNextFloor(
            @Param("topicId") long topicId,
            @Param("userId") long userId
    );

    @Select("SELECT LAST_INSERT_ID()")
    int findAllocatedFloor();

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
            #{floorNo},
            #{userId},
            #{content},
            'NORMAL',
            0
        )
        """)
    int insertReply(
            @Param("replyId") long replyId,
            @Param("topicId") long topicId,
            @Param("floorNo") int floorNo,
            @Param("userId") long userId,
            @Param("content") String content
    );

    @Update("""
        UPDATE reply
        SET content = #{content}, version = version + 1
        WHERE id = #{replyId}
          AND user_id = #{userId}
          AND status = 'NORMAL'
          AND version = #{expectedVersion}
        """)
    int updateReplyContent(
            @Param("replyId") long replyId,
            @Param("userId") long userId,
            @Param("expectedVersion") int expectedVersion,
            @Param("content") String content
    );

    @Update("""
        UPDATE reply
        SET status = 'DELETED', version = version + 1
        WHERE id = #{replyId}
          AND user_id = #{userId}
          AND status = 'NORMAL'
          AND version = #{expectedVersion}
        """)
    int softDeleteReply(
            @Param("replyId") long replyId,
            @Param("userId") long userId,
            @Param("expectedVersion") int expectedVersion
    );

    @Select("""
    SELECT user_id
    FROM topic
        WHERE id = #{topicId}
      AND status = 'NORMAL'
    """)
    Long findTopicAuthorId(@Param("topicId") long topicId);

    @Select("""
    SELECT
        id,
        floor_no AS floorNo,
        user_id AS userId,
        reply_to_floor AS replyToFloor,
        content,
        status,
        version
    FROM reply
    WHERE topic_id = #{topicId}
      AND status = 'NORMAL'
      AND floor_no > #{afterFloor}
    ORDER BY floor_no ASC
    LIMIT #{limit}
    """)
    List<ReplyView> findNextReplies(
            @Param("topicId") long topicId,
            @Param("afterFloor") int afterFloor,
            @Param("limit") int limit
    );

    @Select("""
    SELECT
        id,
        floor_no AS floorNo,
        user_id AS userId,
        reply_to_floor AS replyToFloor,
        content,
        status,
        version
    FROM reply
    WHERE topic_id = #{topicId}
      AND user_id = #{userId}
      AND status = 'NORMAL'
      AND floor_no > #{afterFloor}
    ORDER BY floor_no ASC
    LIMIT #{limit}
    """)
    List<ReplyView> findNextAuthorReplies(
            @Param("topicId") long topicId,
            @Param("userId") long userId,
            @Param("afterFloor") int afterFloor,
            @Param("limit") int limit
    );
}
