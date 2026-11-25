package com.example.agora.identity.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper {
    @Insert("""
        insert into user_account(username, password_hash, trust_level, status) values
        (#{username}, #{password_hash}, 0, 1)
    
    """)
    int insert(@Param("username") String username, @Param("password_hash")String passwordHash);

    @Select("""
        select id, username, password_hash, status, trust_level
        from user_account
        where username = #{username}
    """)
    UserAccountCredential findByUsername(@Param("username") String username);
}
