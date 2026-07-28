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
    private static final ParameterizedTypeReference<List<DashboardResponse.WeeklyQuest>> WEEKLY_QUEST_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.BossRaid>> RAID_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.CharacterProgressionEvent>> PROGRESSION_EVENT_LIST =
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
        List<DashboardResponse.WeeklyQuest> weeklyQuests = backendRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/weekly-quests")
                        .queryParam("weekStartDate", taskDate.with(java.time.DayOfWeek.MONDAY))
                        .build())
                .retrieve()
                .body(WEEKLY_QUEST_LIST);
        DashboardResponse.CharacterProfile character = backendRestClient.get()
                .uri("/api/be/character")
                .retrieve()
                .body(DashboardResponse.CharacterProfile.class);
        List<DashboardResponse.CharacterProgressionEvent> progressionEvents = backendRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/character/progression-events")
                        .queryParam("limit", 8)
                        .build())
                .retrieve()
                .body(PROGRESSION_EVENT_LIST);
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
                List.copyOf(weeklyQuests),
                character,
                List.copyOf(progressionEvents),
                List.copyOf(raids),
                List.copyOf(raidAttempts)
        );
    }

    public DashboardResponse.RaidAttemptResult attemptRaid(long bossRaidId) {
        return backendRestClient.post()
                .uri("/api/be/boss-raids/{bossRaidId}/attempts", bossRaidId)
                .retrieve()
                .body(DashboardResponse.RaidAttemptResult.class);
    }

    public DashboardResponse.Goal createGoal(DashboardController.GoalRequest request) {
        DashboardResponse.Goal goal = backendRestClient.post()
                .uri("/api/be/goals")
                .body(new BackendGoalRequest(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.targetDate()
                ))
                .retrieve()
                .body(DashboardResponse.Goal.class);
        backendRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/goals/{goalId}/recommendations")
                        .queryParam("taskDate", request.taskDate() == null ? LocalDate.now() : request.taskDate())
                        .build(goal.id()))
                .retrieve()
                .toBodilessEntity();
        return goal;
    }

    public DashboardResponse.Goal updateGoal(
            long goalId,
            DashboardController.GoalRequest request
    ) {
        return backendRestClient.put()
                .uri("/api/be/goals/{goalId}", goalId)
                .body(request)
                .retrieve()
                .body(DashboardResponse.Goal.class);
    }

    public void deleteGoal(long goalId) {
        backendRestClient.delete()
                .uri("/api/be/goals/{goalId}", goalId)
                .retrieve()
                .toBodilessEntity();
    }

    public DashboardResponse.DailyTask createDailyTask(
            DashboardController.DailyTaskRequest request
    ) {
        return backendRestClient.post()
                .uri("/api/be/daily-tasks")
                .body(request)
                .retrieve()
                .body(DashboardResponse.DailyTask.class);
    }

    public DashboardResponse.DailyTask updateDailyTask(
            long taskId,
            DashboardController.DailyTaskRequest request
    ) {
        return backendRestClient.put()
                .uri("/api/be/daily-tasks/{taskId}", taskId)
                .body(request)
                .retrieve()
                .body(DashboardResponse.DailyTask.class);
    }

    public void deleteDailyTask(long taskId) {
        backendRestClient.delete()
                .uri("/api/be/daily-tasks/{taskId}", taskId)
                .retrieve()
                .toBodilessEntity();
    }

    public DashboardResponse.WeeklyQuest createWeeklyQuest(
            DashboardController.WeeklyQuestRequest request
    ) {
        return backendRestClient.post()
                .uri("/api/be/weekly-quests")
                .body(request)
                .retrieve()
                .body(DashboardResponse.WeeklyQuest.class);
    }

    public DashboardResponse.WeeklyQuest updateWeeklyQuest(
            long weeklyQuestId,
            DashboardController.WeeklyQuestRequest request
    ) {
        return backendRestClient.put()
                .uri("/api/be/weekly-quests/{weeklyQuestId}", weeklyQuestId)
                .body(request)
                .retrieve()
                .body(DashboardResponse.WeeklyQuest.class);
    }

    public void deleteWeeklyQuest(long weeklyQuestId) {
        backendRestClient.delete()
                .uri("/api/be/weekly-quests/{weeklyQuestId}", weeklyQuestId)
                .retrieve()
                .toBodilessEntity();
    }

    public DashboardResponse.WeeklyQuestCompletionResult completeWeeklyQuest(long weeklyQuestId) {
        return backendRestClient.post()
                .uri("/api/be/weekly-quests/{weeklyQuestId}/complete", weeklyQuestId)
                .retrieve()
                .body(DashboardResponse.WeeklyQuestCompletionResult.class);
    }

    private record BackendGoalRequest(
            String title,
            String description,
            String status,
            LocalDate targetDate
    ) {
    }
}
