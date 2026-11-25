package com.example.agora.identity.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserSessionMapper {

    @Insert("""
        INSERT INTO user_session (user_id, token_hash, expires_at)
        VALUES (#{userId}, #{tokenHash}, #{expiresAt})
        """)
    int insert(
            @Param("userId") long userId,
            @Param("tokenHash") String tokenHash,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Select("""
        SELECT id, user_id as userId, token_hash as tokenHash, expires_at as expiresAt, revoked_at as revokedAt
        FROM user_session
        WHERE token_hash = #{tokenHash}
        """)
    UserSessionCredential findByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Update("""
        UPDATE user_session
        SET revoked_at = #{revokedAt}
        WHERE token_hash = #{tokenHash}
          AND revoked_at IS NULL
        """)
    int revoke(
            @Param("tokenHash") String tokenHash,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}