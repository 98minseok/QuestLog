package com.als98.questlog.bff.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.als98.questlog.bff.config.BackendProperties;
import com.als98.questlog.bff.config.RestClientConfig;
import com.als98.questlog.bff.user.CurrentUserResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(DashboardService.class)
@Import({RestClientConfig.class, CurrentUserResolver.class})
@EnableConfigurationProperties(BackendProperties.class)
class DashboardServiceTests {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MockRestServiceServer backend;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsAuthenticatedJwtToBackendRequests() {
        Jwt jwt = new Jwt(
                "forwarded-token",
                Instant.parse("2026-06-15T00:00:00Z"),
                Instant.parse("2026-06-15T01:00:00Z"),
                Map.of("alg", "none"),
                Map.of("sub", "authenticated-user")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, jwt.getTokenValue(), "ROLE_USER")
        );
        backend.expect(once(), requestTo("http://localhost:8081/api/be/boss-raids/3/attempts"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer forwarded-token"))
                .andRespond(withSuccess("""
                        {
                          "attemptId":7,
                          "bossRaidId":3,
                          "bossName":"Deadline Dragon",
                          "stage":1,
                          "status":"VICTORY",
                          "damageDealt":100,
                          "xpAwarded":50,
                          "totalXp":50,
                          "level":1,
                          "strength":1,
                          "vitality":1
                        }
                        """, MediaType.APPLICATION_JSON));

        dashboardService.attemptRaid(3);

        backend.verify();
    }

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

    @Test
    void proxiesRaidAttemptAndMapsProgressionResult() {
        backend.expect(once(), requestTo("http://localhost:8081/api/be/boss-raids/3/attempts"))
                .andExpect(method(POST))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("""
                        {
                          "attemptId":7,
                          "bossRaidId":3,
                          "bossName":"Deadline Dragon",
                          "stage":1,
                          "status":"VICTORY",
                          "damageDealt":100,
                          "xpAwarded":50,
                          "totalXp":175,
                          "level":2,
                          "strength":2,
                          "vitality":2
                        }
                        """, MediaType.APPLICATION_JSON));

        DashboardResponse.RaidAttemptResult result = dashboardService.attemptRaid(3);

        assertThat(result.status()).isEqualTo("VICTORY");
        assertThat(result.xpAwarded()).isEqualTo(50);
        assertThat(result.totalXp()).isEqualTo(175);
        backend.verify();
    }

    @Test
    void proxiesGoalUpdateAndDelete() {
        DashboardController.GoalRequest request = new DashboardController.GoalRequest(
                "Ship MVP",
                "Finish the playable loop",
                "ARCHIVED",
                LocalDate.of(2026, 7, 1)
        );
        backend.expect(once(), requestTo("http://localhost:8081/api/be/goals/3"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {
                          "title":"Ship MVP",
                          "description":"Finish the playable loop",
                          "status":"ARCHIVED",
                          "targetDate":"2026-07-01"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":3,
                          "title":"Ship MVP",
                          "description":"Finish the playable loop",
                          "status":"ARCHIVED",
                          "targetDate":"2026-07-01",
                          "createdAt":"2026-06-14T09:00:00Z",
                          "updatedAt":"2026-06-15T09:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));
        backend.expect(once(), requestTo("http://localhost:8081/api/be/goals/3"))
                .andExpect(method(DELETE))
                .andRespond(withNoContent());

        DashboardResponse.Goal updated = dashboardService.updateGoal(3, request);
        dashboardService.deleteGoal(3);

        assertThat(updated.status()).isEqualTo("ARCHIVED");
        backend.verify();
    }

    @Test
    void proxiesDailyTaskUpdateAndDelete() {
        DashboardController.DailyTaskRequest request = dailyTaskUpdateRequest("SKIPPED");
        backend.expect(once(), requestTo("http://localhost:8081/api/be/daily-tasks/7"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {
                          "goalId":3,
                          "title":"Polish dashboard",
                          "description":"Finish responsive behavior",
                          "taskDate":"2026-06-15",
                          "status":"SKIPPED",
                          "xpReward":25
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":7,
                          "goalId":3,
                          "title":"Polish dashboard",
                          "description":"Finish responsive behavior",
                          "taskDate":"2026-06-15",
                          "status":"SKIPPED",
                          "source":"MANUAL",
                          "xpReward":25,
                          "createdAt":"2026-06-15T09:00:00Z",
                          "updatedAt":"2026-06-15T10:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));
        backend.expect(once(), requestTo("http://localhost:8081/api/be/daily-tasks/7"))
                .andExpect(method(DELETE))
                .andRespond(withNoContent());

        DashboardResponse.DailyTask updated = dashboardService.updateDailyTask(7, request);
        dashboardService.deleteDailyTask(7);

        assertThat(updated.status()).isEqualTo("SKIPPED");
        backend.verify();
    }

    @Test
    void forwardsOmittedDailyTaskStatusWithoutDefaultingIt() {
        DashboardController.DailyTaskRequest request = dailyTaskUpdateRequest(null);
        backend.expect(once(), requestTo("http://localhost:8081/api/be/daily-tasks/7"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {
                          "goalId":3,
                          "title":"Polish dashboard",
                          "description":"Finish responsive behavior",
                          "taskDate":"2026-06-15",
                          "status":null,
                          "xpReward":25
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":7,
                          "goalId":3,
                          "title":"Polish dashboard",
                          "description":"Finish responsive behavior",
                          "taskDate":"2026-06-15",
                          "status":"PENDING",
                          "source":"MANUAL",
                          "xpReward":25,
                          "createdAt":"2026-06-15T09:00:00Z",
                          "updatedAt":"2026-06-15T10:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        DashboardResponse.DailyTask updated = dashboardService.updateDailyTask(7, request);

        assertThat(updated.status()).isEqualTo("PENDING");
        backend.verify();
    }

    private DashboardController.DailyTaskRequest dailyTaskUpdateRequest(String status) {
        return new DashboardController.DailyTaskRequest(
                3L,
                "Polish dashboard",
                "Finish responsive behavior",
                LocalDate.of(2026, 6, 15),
                status,
                25
        );
    }

    private void expectGet(String path, String responseBody) {
        backend.expect(once(), requestTo("http://localhost:8081" + path))
                .andExpect(method(GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }
}
