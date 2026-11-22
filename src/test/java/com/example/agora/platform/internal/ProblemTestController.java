package com.example.agora.platform.internal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.example.agora.platform.api.ApiException;



@RestController
public class ProblemTestController {

    @GetMapping("/test/problem")
    public void problem() {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Test resource was not found"
        );
    }

    @GetMapping("/test/api-problem")
    public void apiProblem() {
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_INPUT",
                "The request is invalid"
        );
    }
}