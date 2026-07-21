package com.als98.questlog.bff.dashboard;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                dailyTaskUpdateRequest("Reactivate skipped task", "PENDING"),
                "Skipped daily tasks cannot be edited"
        );
    }

    @Test
    void proxiesRecommendationPreviewWithoutCreatingTasks() throws Exception {
        backend.expect(once(), requestTo(
                        "http://localhost:8081/api/be/goals/9/recommendations/preview?taskDate=2026-06-16"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [{
                          "goalId":9,
                          "title":"Plan the next step for Study Korean",
                          "description":"Write one concrete outcome.",
                          "taskDate":"2026-06-16",
                          "xpReward":10,
                          "source":"AI_RECOMMENDED"
                        }]
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/bff/goals/9/recommendations/preview")
                        .param("taskDate", "2026-06-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalId").value(9))
                .andExpect(jsonPath("$[0].title").value("Plan the next step for Study Korean"))
                .andExpect(jsonPath("$[0].source").value("AI_RECOMMENDED"));

        backend.verify();
    }

    @Test
    void proxiesSelectedRecommendationDraftAcceptance() throws Exception {
        backend.expect(once(), requestTo(
                        "http://localhost:8081/api/be/goals/9/recommendations/accept"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        [{
                          "id":21,
                          "goalId":9,
                          "title":"Rehearse the three-minute demo",
                          "description":"Practice the edited pitch flow.",
                          "taskDate":"2026-06-16",
                          "status":"PENDING",
                          "source":"AI_RECOMMENDED",
                          "xpReward":35,
                          "createdAt":"2026-06-16T09:00:00Z",
                          "updatedAt":"2026-06-16T09:00:00Z"
                        }]
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/bff/goals/9/recommendations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{
                                  "title":"Rehearse the three-minute demo",
                                  "description":"Practice the edited pitch flow.",
                                  "taskDate":"2026-06-16",
                                  "xpReward":35
                                }]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalId").value(9))
                .andExpect(jsonPath("$[0].title").value("Rehearse the three-minute demo"))
                .andExpect(jsonPath("$[0].source").value("AI_RECOMMENDED"));

        backend.verify();
    }

    @Test
    void proxiesRecommendationHistoryForAGoal() throws Exception {
        backend.expect(once(), requestTo(
                        "http://localhost:8081/api/be/goals/9/recommendations/history?limit=5"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [{
                          "id":31,
                          "goalId":9,
                          "createdTaskId":21,
                          "provider":"deterministic-mock",
                          "action":"ACCEPTED",
                          "title":"Rehearse the three-minute demo",
                          "description":"Practice the edited pitch flow.",
                          "taskDate":"2026-06-16",
                          "xpReward":35,
                          "source":"AI_RECOMMENDED",
                          "createdAt":"2026-06-16T09:00:00Z"
                        }]
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/bff/goals/9/recommendations/history")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalId").value(9))
                .andExpect(jsonPath("$[0].createdTaskId").value(21))
                .andExpect(jsonPath("$[0].provider").value("deterministic-mock"))
                .andExpect(jsonPath("$[0].action").value("ACCEPTED"));

        backend.verify();
    }

    @Test
    void forwardsDirectCompletionUpdateRejectionFromBackend() throws Exception {
        assertForwardedError(
                PUT,
                HttpStatus.BAD_REQUEST,
                "/api/be/daily-tasks/7",
                dailyTaskUpdateRequest("Complete through reward flow", "COMPLETED"),
                "Use the daily task completion endpoint to complete a task"
        );
    }

    private MockHttpServletRequestBuilder dailyTaskUpdateRequest(String title, String status) {
        return put("/api/bff/daily-tasks/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "goalId": 3,
                          "title": "%s",
                          "taskDate": "2026-06-15",
                          "status": "%s",
                          "xpReward": 25
                        }
                        """.formatted(title, status));
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
