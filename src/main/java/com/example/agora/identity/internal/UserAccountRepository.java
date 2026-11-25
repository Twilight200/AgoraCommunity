package com.example.agora.identity.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserAccountRepository {

    private final UserAccountMapper userAccountMapper;


    @Autowired
    public UserAccountRepository(UserAccountMapper userAccountMapper){
        this.userAccountMapper = userAccountMapper;
    }

    void createAccount(String username, String passwordHash){
        int affectRows = userAccountMapper.insert(username,passwordHash);

        if(affectRows != 1){
            throw new IllegalStateException("Creating an count must affect exactly one row.");
        }
    }

    Optional<UserAccountCredential> findByUsername(String name){
        return Optional.ofNullable(userAccountMapper.findByUsername(name));
    }

    public Optional<Integer> findTrustLevelById(long userId){
        return Optional.ofNullable(userAccountMapper.findTrustLevelById(userId));
    }

}
