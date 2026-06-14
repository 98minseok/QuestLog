package com.als98.questlog.bff.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class BackendApiExceptionHandlerTests {

    private final BackendApiExceptionHandler handler = new BackendApiExceptionHandler();

    @Test
    void preservesBackendStatusContentTypeAndBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.CONFLICT,
                "Conflict",
                headers,
                """
                {"message":"Daily task has already been completed"}
                """.strip().getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        ResponseEntity<byte[]> response = handler.backendError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody())
                .asString(StandardCharsets.UTF_8)
                .isEqualTo("{\"message\":\"Daily task has already been completed\"}");
    }

    @Test
    void preservesEmptyBackendErrorBody() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        ResponseEntity<byte[]> response = handler.backendError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEmpty();
        assertThat(response.getHeaders().getContentType()).isNull();
    }
}
