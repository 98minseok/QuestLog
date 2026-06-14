package com.als98.questlog.bff.api;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class BackendApiExceptionHandler {

    private static final Map<String, String> BACKEND_UNAVAILABLE =
            Map.of("message", "The QuestLog backend is unavailable.");
    private static final String VALIDATION_FAILED = "Request validation failed.";

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<byte[]> backendError(RestClientResponseException exception) {
        return new ResponseEntity<>(
                exception.getResponseBodyAsByteArray(),
                forwardedHeaders(exception),
                exception.getStatusCode()
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, String>> backendUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BACKEND_UNAVAILABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validationFailed(
            MethodArgumentNotValidException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", firstValidationMessage(exception)));
    }

    private String firstValidationMessage(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(VALIDATION_FAILED);
    }

    private HttpHeaders forwardedHeaders(RestClientResponseException exception) {
        HttpHeaders headers = new HttpHeaders();
        HttpHeaders backendHeaders = exception.getResponseHeaders();
        if (backendHeaders != null && backendHeaders.getContentType() != null) {
            headers.setContentType(backendHeaders.getContentType());
        }
        return headers;
    }
}
