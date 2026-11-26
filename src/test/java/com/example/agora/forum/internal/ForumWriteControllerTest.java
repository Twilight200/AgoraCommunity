package com.example.agora.forum.internal;

import com.example.agora.identity.api.SessionApi;
import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ForumWriteController.class,
        properties = "spring.mvc.problemdetails.enabled=true"
)
class ForumWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionApi sessionApi;

    @MockitoBean
    private ForumTopicService topicService;

    @MockitoBean
    private ForumReplyService replyService;

    @Test
    void createsTopicUsingAuthenticatedUser() throws Exception {
        when(sessionApi.authenticate("valid-token"))
                .thenReturn(42L);
        when(topicService.createTopic(
                7L,
                42L,
                "A topic",
                "Topic content"
        )).thenReturn(900L);

        mockMvc.perform(post("/api/forum/topics")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 7,
                                  "title": "A topic",
                                  "content": "Topic content"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.topicId").value(900));

        verify(sessionApi).authenticate("valid-token");
        verify(topicService).createTopic(
                7L,
                42L,
                "A topic",
                "Topic content"
        );
        verifyNoMoreInteractions(sessionApi, topicService);
        verifyNoInteractions(replyService);
    }

    @Test
    void createsReplyUsingAuthenticatedUser() throws Exception {
        when(sessionApi.authenticate("valid-token"))
                .thenReturn(42L);
        when(replyService.createReply(
                900L,
                42L,
                "A reply"
        )).thenReturn(new CreatedReply(901L, 2));

        mockMvc.perform(post("/api/forum/topics/900/replies")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "A reply"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.replyId").value(901))
                .andExpect(jsonPath("$.data.floorNo").value(2));

        verify(sessionApi).authenticate("valid-token");
        verify(replyService).createReply(900L, 42L, "A reply");
        verifyNoMoreInteractions(sessionApi, replyService);
        verifyNoInteractions(topicService);
    }

    @Test
    void rejectsMissingAuthorizationBeforeCallingServices() throws Exception {
        mockMvc.perform(post("/api/forum/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 7,
                                  "title": "A topic",
                                  "content": "Topic content"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("IDENTITY_INVALID_SESSION"));

        verifyNoInteractions(sessionApi, topicService, replyService);
    }

    @Test
    void returnsUnauthorizedWhenSessionApiRejectsToken() throws Exception {
        when(sessionApi.authenticate("expired-token"))
                .thenThrow(new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "IDENTITY_INVALID_SESSION",
                        "session is invalid"
                ));

        mockMvc.perform(post("/api/forum/topics/900/replies")
                        .header("Authorization", "Bearer expired-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "A reply"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("IDENTITY_INVALID_SESSION"));

        verify(sessionApi).authenticate("expired-token");
        verifyNoInteractions(topicService, replyService);
    }

    @Test
    void rejectsInvalidTopicRequestBeforeCallingServices() throws Exception {
        mockMvc.perform(post("/api/forum/topics")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 0,
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sessionApi, topicService, replyService);
    }
}