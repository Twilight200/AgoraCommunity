package com.example.agora.forum.internal;

import com.example.agora.identity.api.SessionApi;
import com.example.agora.platform.api.ApiException;
import com.example.agora.platform.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
public class ForumWriteController {
    private final ForumReplyService replyService;
    private final ForumTopicService topicService;
    private final SessionApi sessionApi;

    public ForumWriteController(ForumReplyService replyService, ForumTopicService topicService, SessionApi sessionApi){
        this.replyService = replyService;
        this.sessionApi = sessionApi;
        this.topicService = topicService;
    }

    @PostMapping("/topics")
    public ResponseEntity<ApiResponse<CreatedTopic>> createTopic(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateTopicRequest request
    ){
        long userId = authenticate(authorization);

        long topicId = topicService.createTopic(request.categoryId(), userId, request.title(), request.content());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new CreatedTopic(topicId)));
    }

    @PostMapping("/topics/{topicId}/replies")
    public ResponseEntity<ApiResponse<CreatedReply>> createReply(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable long topicId,
            @Valid @RequestBody CreateReplyRequest request
    ){
        long userId = authenticate(authorization);

        CreatedReply createdReply = replyService.createReply(topicId, userId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdReply));


    }

    private long authenticate(String authorization){
        String prefix = "Bearer ";

        if(authorization == null || !authorization.startsWith(prefix) || authorization.substring(prefix.length()).isBlank()){
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "IDENTITY_INVALID_SESSION",
                    "session is invalid"
            );
        }

        String rawToken = authorization.substring(prefix.length());
        return sessionApi.authenticate(rawToken);
    }
}
