package com.als98.questlog.bff.dashboard;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.als98.questlog.bff.api.BackendApiExceptionHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardControllerValidationTests {

    private final DashboardService dashboardService = mock(DashboardService.class);
    private final RestClient backendRestClient = mock(RestClient.class);
    private MockMvc mockMvc;

    @BeforeAll
    void setUpMockMvc() {
        DashboardController controller =
                new DashboardController(dashboardService, backendRestClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BackendApiExceptionHandler())
                .build();
    }

    @Test
    void rejectsBlankGoalTitleBeforeCallingBackend() throws Exception {
        assertRejected("/api/bff/goals", """
                {
                  "title":" ",
                  "description":"Invalid goal"
                }
                """);
    }

    @Test
    void rejectsInvalidDailyTaskBeforeCallingBackend() throws Exception {
        assertRejected("/api/bff/daily-tasks", """
                {
                  "title":"Invalid task",
                  "xpReward":0
                }
                """);
    }

    private void assertRejected(String path, String requestBody) throws Exception {
        mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(dashboardService, backendRestClient);
    }
}
