package com.example.agora.identity.internal;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface UserDailyQuotaMapper {

    @Insert("""
    INSERT INTO user_daily_quota (
        user_id,
        quota_date,
        topics_created,
        replies_created
    )
    VALUES (
        #{userId},
        #{quotaDate},
        0,
        0
    )
    ON DUPLICATE KEY UPDATE
        quota_date = VALUES(quota_date)
    """)
    int insertIfAbsent(@Param("userId") long userId, @Param("quotaDate")LocalDate quotaDate);

    @Select("""
        SELECT topics_created AS topicsCreated,
               replies_created AS repliesCreated
        FROM user_daily_quota
        WHERE user_id = #{userId}
          AND quota_date = #{quotaDate}
        FOR UPDATE
        """)
    UserDailyQuotaCount findForUpdate(@Param("userId") long userId, @Param("quotaDate") LocalDate quotaDate);

    @Update("""
        UPDATE user_daily_quota
        SET topics_created = topics_created + 1
        WHERE user_id = #{userId}
          AND quota_date = #{quotaDate}
        """)
    int incrementTopics(
            @Param("userId") long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Update("""
        UPDATE user_daily_quota
        SET replies_created = replies_created + 1
        WHERE user_id = #{userId}
          AND quota_date = #{quotaDate}
        """)
    int incrementReplies(
            @Param("userId") long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

}
