package com.example.agora.forum.internal;


import com.example.agora.platform.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/topics")
public class ForumReplyQueryController {

    private final ForumReplyQueryService queryService;

    public ForumReplyQueryController(ForumReplyQueryService queryService){
        this.queryService = queryService;
    }

    @GetMapping("/{topicId}/replies")
    public ResponseEntity<ApiResponse<ReplyPage>> readReplies(
            @PathVariable long topicId,
            @RequestParam(required = false) Integer afterFloor,
            @RequestParam(defaultValue = "false") boolean onlyAuthor
    ){
        ReplyPage page = !onlyAuthor
                ? queryService.readReplies(topicId, afterFloor)
                : queryService.readAuthorReplies(topicId,afterFloor);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(page));
    }
}
