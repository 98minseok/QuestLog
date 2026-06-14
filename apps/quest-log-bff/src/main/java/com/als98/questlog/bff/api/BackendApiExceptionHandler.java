package com.als98.questlog.bff.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class BackendApiExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<byte[]> backendError(RestClientResponseException exception) {
        return new ResponseEntity<>(
                exception.getResponseBodyAsByteArray(),
                forwardedHeaders(exception),
                exception.getStatusCode()
        );
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
