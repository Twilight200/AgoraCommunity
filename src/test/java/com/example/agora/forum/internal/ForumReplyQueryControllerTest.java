package com.example.agora.forum.internal;

import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ForumReplyQueryController.class,
        properties = "spring.mvc.problemdetails.enabled=true"
)
class ForumReplyQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ForumReplyQueryService queryService;

    @Test
    void readsRepliesWithCursorAndWrapsResult() throws Exception {
        ReplyPage page = new ReplyPage(
                77L,
                12,
                35,
                true,
                false,
                List.of(new ReplyView(
                        9001L,
                        20,
                        300L,
                        null,
                        "A reply",
                        "NORMAL",
                        0
                ))
        );

        when(queryService.readReplies(77L, 12))
                .thenReturn(page);

        mockMvc.perform(get("/api/forum/topics/77/replies")
                        .param("afterFloor", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(77))
                .andExpect(jsonPath("$.data.afterFloor").value(12))
                .andExpect(jsonPath("$.data.nextCursor").value(35))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.onlyAuthor").value(false))
                .andExpect(jsonPath("$.data.replies[0].floorNo").value(20))
                .andExpect(jsonPath("$.data.replies[0].content").value("A reply"));

        verify(queryService).readReplies(77L, 12);
        verifyNoMoreInteractions(queryService);
        verifyNoInteractionsBeyondReadReplies();
    }

    @Test
    void routesOnlyAuthorRequestToAuthorQuery() throws Exception {
        ReplyPage page = new ReplyPage(
                77L,
                0,
                null,
                false,
                true,
                List.of()
        );

        when(queryService.readAuthorReplies(77L, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/forum/topics/77/replies")
                        .param("onlyAuthor", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onlyAuthor").value(true))
                .andExpect(jsonPath("$.data.replies").isEmpty());

        verify(queryService).readAuthorReplies(77L, null);
        verifyNoMoreInteractions(queryService);
    }

    @Test
    void returnsBadRequestProblemForNegativeCursor() throws Exception {
        doThrow(new ApiException(
                HttpStatus.BAD_REQUEST,
                "FORUM_INVALID_CURSOR",
                "cursor must not be negative"
        )).when(queryService).readReplies(77L, -1);

        mockMvc.perform(get("/api/forum/topics/77/replies")
                        .param("afterFloor", "-1")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("FORUM_INVALID_CURSOR"));

        verify(queryService).readReplies(77L, -1);
    }

    private void verifyNoInteractionsBeyondReadReplies() {
        // The first test verifies its expected Service call above.
        // Add a stricter Mockito check here if your Mockito version exposes
        // only the interaction verification style used in your project.
    }
}