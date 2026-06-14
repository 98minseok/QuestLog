package com.als98.questlog.bff.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendApiExceptionHandlerTests {

    private final BackendApiExceptionHandler handler = new BackendApiExceptionHandler();
    private MockMvc mockMvc;

    @BeforeAll
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingBackendController())
                .setControllerAdvice(handler)
                .build();
    }

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

    @Test
    void returnsStableServiceUnavailableResponseWhenBackendCannotBeReached() {
        ResponseEntity<?> response = handler.backendUnavailable();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .isEqualTo(Map.of(
                        "message",
                        "The QuestLog backend is unavailable."
                ));
    }

    @Test
    void forwardsBackendErrorThroughMvc() throws Exception {
        mockMvc.perform(get("/test/backend-conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Raid has already been cleared"));
    }

    @Test
    void mapsBackendConnectionFailureThroughMvc() throws Exception {
        mockMvc.perform(get("/test/backend-offline"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("The QuestLog backend is unavailable."));
    }

    @RestController
    private static class FailingBackendController {

        @GetMapping("/test/backend-conflict")
        void backendConflict() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            throw HttpClientErrorException.create(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    headers,
                    """
                    {"message":"Raid has already been cleared"}
                    """.strip().getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );
        }

        @GetMapping("/test/backend-offline")
        void backendOffline() {
            throw new ResourceAccessException("Connection refused");
        }
    }
}
