package com.als98.questlog.bff.dashboard;

import java.time.LocalDate;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DashboardService {

    private static final ParameterizedTypeReference<List<DashboardResponse.Goal>> GOAL_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.DailyTask>> TASK_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.BossRaid>> RAID_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.RaidAttempt>> ATTEMPT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient backendRestClient;

    public DashboardService(RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    public DashboardResponse getDashboard(LocalDate taskDate) {
        List<DashboardResponse.Goal> goals = backendRestClient.get()
                .uri("/api/be/goals")
                .retrieve()
                .body(GOAL_LIST);
        List<DashboardResponse.DailyTask> dailyTasks = backendRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/daily-tasks")
                        .queryParam("taskDate", taskDate)
                        .build())
                .retrieve()
                .body(TASK_LIST);
        DashboardResponse.CharacterProfile character = backendRestClient.get()
                .uri("/api/be/character")
                .retrieve()
                .body(DashboardResponse.CharacterProfile.class);
        List<DashboardResponse.BossRaid> raids = backendRestClient.get()
                .uri("/api/be/boss-raids")
                .retrieve()
                .body(RAID_LIST);
        List<DashboardResponse.RaidAttempt> raidAttempts = backendRestClient.get()
                .uri("/api/be/raid-attempts")
                .retrieve()
                .body(ATTEMPT_LIST);

        return new DashboardResponse(
                taskDate,
                List.copyOf(goals),
                List.copyOf(dailyTasks),
                character,
                List.copyOf(raids),
                List.copyOf(raidAttempts)
        );
    }
}
