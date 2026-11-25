package com.example.agora.identity.internal;

import com.example.agora.identity.api.LoginRequest;
import com.example.agora.identity.api.LoginResponse;
import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.identity.api.RegisterResponse;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identity")
public class UserAccountController {
    private final UserAccountService userAccountService;
    private final SessionService sessionService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService, SessionService sessionService){
        this.userAccountService = userAccountService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest registerRequest){
        userAccountService.register(registerRequest);

        var response = new RegisterResponse(registerRequest.username());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<LoginResponse>>login(@Valid @RequestBody LoginRequest request){
        var result = userAccountService.login(request);

        var response = new LoginResponse(result.accessToken(), "Bearer", result.expiresAt());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = "Authorization", required = false)
            String authorization
    ){
        String rawToken = extractBearerToken(authorization);
        sessionService.revoke(rawToken);
        return ResponseEntity.ok(ApiResponse.success(null));

    }

    private String extractBearerToken(String authorization){
        if(authorization == null || !authorization.startsWith("Bearer ")){
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "IDENTITY_INVALID_SESSION",
                    "session is invalid"
            );
        }
        return authorization.substring("Bearer ".length());
    }
}
