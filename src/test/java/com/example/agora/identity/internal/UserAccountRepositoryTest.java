package com.example.agora.identity.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountRepositoryTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @InjectMocks
    private UserAccountRepository userAccountRepository;

    @Test
    void createsAccountThroughMapper() {
        when(userAccountMapper.insert("agora_user", "bcrypt-hash"))
                .thenReturn(1);

        userAccountRepository.createAccount("agora_user", "bcrypt-hash");

        verify(userAccountMapper).insert("agora_user", "bcrypt-hash");
    }
}