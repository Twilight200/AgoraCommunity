package com.example.agora.platform.internal;

import com.example.agora.platform.api.ApiException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                exception.getStatus(),
                exception.getMessage()
        );

        problemDetail.setTitle("Request failed");
        problemDetail.setProperty("code",exception.getCode());
        return problemDetail;
    }

}
