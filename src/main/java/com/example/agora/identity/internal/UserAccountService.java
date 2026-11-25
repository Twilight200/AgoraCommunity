package com.example.agora.identity.internal;

import com.example.agora.identity.api.LoginRequest;
import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.platform.api.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
    }

    public void register(RegisterRequest request){
        int passwordBytes = request.password().getBytes(StandardCharsets.UTF_8).length;

        if(passwordBytes > 72){
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDENTITY_PASSWORD_TOO_LONG", "password must not exceed 72 UTF-8 bytes");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        try{
            userAccountRepository.createAccount(request.username(), passwordHash);
        }catch (DuplicateKeyException exception){
            throw new ApiException(HttpStatus.CONFLICT,"IDENTITY_USERNAME_TAKEN", "username is already taken");
        }
    }

    public long authenticate(LoginRequest request){
        var account = userAccountRepository.findByUsername(request.username());

        String passwordHash = account.map(UserAccountCredential::passwordHash).orElse(DUMMY_PASSWORD_HASH);

        boolean passwordMatches = passwordEncoder.matches(request.password(),passwordHash);

        if(account.isEmpty() || !passwordMatches || account.get().status() != 1){
            throw new ApiException(HttpStatus.UNAUTHORIZED, "IDENTITY_INVALID_CREDENTIALS", "username or password is invalid");
        }

        return account.get().id();
    }

    public LoginResult login(LoginRequest request){
        long userId = authenticate(request);

        var issuedToken = sessionTokenService.issue();

        userSessionRepository.create(
                userId,
                issuedToken.tokenHash(),
                java.time.LocalDateTime.ofInstant(
                        issuedToken.expiresAt(),
                        ZoneOffset.UTC
                )
        );

        return new LoginResult(issuedToken.rawToken(), issuedToken.expiresAt());
    }

}
