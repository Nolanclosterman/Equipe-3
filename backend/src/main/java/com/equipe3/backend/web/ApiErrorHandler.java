package com.equipe3.backend.web;

import com.equipe3.backend.web.ApiExceptions.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Returns validation errors with their message in the body, so the frontend
 * can show the kid-friendly explanation (e.g. the name moderation message)
 * instead of a generic "server error".
 */
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(BadRequestException e) {
        return Map.of("message", e.getMessage());
    }
}
