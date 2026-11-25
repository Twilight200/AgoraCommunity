package com.example.agora.identity.internal;

import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.identity.api.LoginRequest;
import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserAccountController.class,
        properties = "spring.mvc.problemdetails.enabled=true"
)
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void registersValidRequestAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "agora_user",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.data.username").value("agora_user"));

        verify(userAccountService).register(any(RegisterRequest.class));
    }

    @Test
    void rejectsInvalidRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "abc",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userAccountService);
    }

    @Test
    void returnsProblemDetailsWhenUsernameIsTaken() throws Exception {
        doThrow(new ApiException(
                HttpStatus.CONFLICT,
                "IDENTITY_USERNAME_TAKEN",
                "Username is already taken"
        )).when(userAccountService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content("""
                                {
                                  "username": "agora_user",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code")
                        .value("IDENTITY_USERNAME_TAKEN"));
    }

    @Test
    void logsInAndReturnsBearerToken() throws Exception {
        var loginResult = new LoginResult(
                "raw-access-token",
                java.time.Instant.parse("2026-11-24T00:00:00Z")
        );
        org.mockito.Mockito.when(userAccountService.login(any(LoginRequest.class)))
                .thenReturn(loginResult);

        mockMvc.perform(post("/api/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "agora_user",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("raw-access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        verify(userAccountService).login(any(LoginRequest.class));
    }

    @Test
    void logoutPassesBearerTokenWithoutPrefixOrWhitespace() throws Exception {
        mockMvc.perform(post("/api/identity/logout")
                        .header("Authorization", "Bearer raw-access-token"))
                .andExpect(status().isOk());

        verify(sessionService).revoke("raw-access-token");
    }

    @Test
    void rejectsLogoutHeaderWithoutBearerScheme() throws Exception {
        mockMvc.perform(post("/api/identity/logout")
                        .header("Authorization", "Basic raw-access-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sessionService);
    }
}
