package com.als98.questlog.bff.dashboard;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.als98.questlog.bff.api.BackendApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

class DashboardErrorForwardingTests {

    private MockRestServiceServer backend;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestClient.Builder backendBuilder = RestClient.builder()
                .baseUrl("http://localhost:8081");
        backend = MockRestServiceServer.bindTo(backendBuilder).build();
        RestClient backendClient = backendBuilder.build();
        DashboardController controller = new DashboardController(
                new DashboardService(backendClient),
                backendClient
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BackendApiExceptionHandler())
                .build();
    }

    @Test
    void forwardsGoalDeletionConflictFromBackend() throws Exception {
        assertForwardedError(
                DELETE,
                HttpStatus.CONFLICT,
                "/api/be/goals/3",
                delete("/api/bff/goals/3"),
                "Goal 3 cannot be deleted while it has daily tasks"
        );
    }

    @Test
    void forwardsDailyTaskDeletionConflictFromBackend() throws Exception {
        assertForwardedError(
                DELETE,
                HttpStatus.CONFLICT,
                "/api/be/daily-tasks/7",
                delete("/api/bff/daily-tasks/7"),
                "Daily task 7 cannot be deleted from status COMPLETED"
        );
    }

    @Test
    void forwardsSkippedTaskCompletionConflictFromBackend() throws Exception {
        assertForwardedError(
                POST,
                HttpStatus.CONFLICT,
                "/api/be/daily-tasks/7/complete",
                post("/api/bff/daily-tasks/7/complete"),
                "Daily task 7 cannot be completed from status SKIPPED"
        );
    }

    @Test
    void forwardsSkippedTaskEditRejectionFromBackend() throws Exception {
        assertForwardedError(
                PUT,
                HttpStatus.BAD_REQUEST,
                "/api/be/daily-tasks/7",
                put("/api/bff/daily-tasks/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": 3,
                                  "title": "Reactivate skipped task",
                                  "taskDate": "2026-06-15",
                                  "status": "PENDING",
                                  "xpReward": 25
                                }
                                """),
                "Skipped daily tasks cannot be edited"
        );
    }

    private void assertForwardedError(
            HttpMethod httpMethod,
            HttpStatus httpStatus,
            String backendPath,
            MockHttpServletRequestBuilder bffRequest,
            String message
    ) throws Exception {
        backend.expect(once(), requestTo("http://localhost:8081" + backendPath))
                .andExpect(method(httpMethod))
                .andRespond(withStatus(httpStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"" + message + "\"}"));

        mockMvc.perform(bffRequest)
                .andExpect(status().is(httpStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(message));

        backend.verify();
    }
}
