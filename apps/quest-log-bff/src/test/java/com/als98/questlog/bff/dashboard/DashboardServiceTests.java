package com.als98.questlog.bff.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.als98.questlog.bff.config.BackendProperties;
import com.als98.questlog.bff.config.RestClientConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(DashboardService.class)
@Import(RestClientConfig.class)
@EnableConfigurationProperties(BackendProperties.class)
class DashboardServiceTests {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MockRestServiceServer backend;

    @Test
    void aggregatesDashboardResourcesForRequestedDate() {
        LocalDate taskDate = LocalDate.of(2026, 6, 14);
        expectGet("/api/be/goals", """
                [{"id":1,"title":"Ship MVP","description":"Finish the dashboard","status":"ACTIVE",
                  "targetDate":"2026-06-30","createdAt":"2026-06-01T09:00:00Z",
                  "updatedAt":"2026-06-01T09:00:00Z"}]
                """);
        expectGet("/api/be/daily-tasks?taskDate=2026-06-14", """
                [{"id":2,"goalId":1,"title":"Add BFF","description":null,"taskDate":"2026-06-14",
                  "status":"PENDING","source":"AI_RECOMMENDED","xpReward":20,
                  "createdAt":"2026-06-14T09:00:00Z","updatedAt":"2026-06-14T09:00:00Z"}]
                """);
        expectGet("/api/be/character", """
                {"userId":1,"displayName":"Dev Hero","level":2,"totalXp":125,"currentLevelXp":25,
                 "xpToNextLevel":75,"strength":2,"vitality":2}
                """);
        expectGet("/api/be/boss-raids", """
                [{"id":3,"stage":1,"name":"Deadline Dragon","requiredLevel":1,"maxHp":100,
                  "xpReward":50,"active":true,"unlocked":true}]
                """);
        expectGet("/api/be/raid-attempts", """
                [{"id":4,"bossRaidId":3,"bossName":"Deadline Dragon","stage":1,"status":"CLEARED",
                  "damageDealt":100,"startedAt":"2026-06-14T10:00:00Z",
                  "completedAt":"2026-06-14T10:05:00Z"}]
                """);

        DashboardResponse dashboard = dashboardService.getDashboard(taskDate);

        assertThat(dashboard.taskDate()).isEqualTo(taskDate);
        assertThat(dashboard.goals()).singleElement()
                .extracting(DashboardResponse.Goal::title)
                .isEqualTo("Ship MVP");
        assertThat(dashboard.dailyTasks()).singleElement()
                .extracting(DashboardResponse.DailyTask::source)
                .isEqualTo("AI_RECOMMENDED");
        assertThat(dashboard.character().totalXp()).isEqualTo(125);
        assertThat(dashboard.raids()).singleElement()
                .extracting(DashboardResponse.BossRaid::unlocked)
                .isEqualTo(true);
        assertThat(dashboard.raidAttempts()).hasSize(1);
        backend.verify();
    }

    private void expectGet(String path, String responseBody) {
        backend.expect(once(), requestTo("http://localhost:8081" + path))
                .andExpect(method(GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }
}
