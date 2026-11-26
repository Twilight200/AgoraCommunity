package com.example.agora.forum.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
}