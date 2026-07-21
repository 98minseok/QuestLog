package com.als98.questlog.bff.dashboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/bff")
public class DashboardController {

    private static final ParameterizedTypeReference<List<DashboardResponse.DailyTask>> TASK_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<DashboardResponse.RecommendationDraft>> RECOMMENDATION_DRAFT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final DashboardService dashboardService;
    private final RestClient backendRestClient;

    public DashboardController(DashboardService dashboardService, RestClient backendRestClient) {
        this.dashboardService = dashboardService;
        this.backendRestClient = backendRestClient;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return dashboardService.getDashboard(taskDate == null ? LocalDate.now() : taskDate);
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse.Goal createGoal(@Valid @RequestBody GoalRequest request) {
        return dashboardService.createGoal(request);
    }

    @PutMapping("/goals/{goalId}")
    public DashboardResponse.Goal updateGoal(
            @PathVariable long goalId,
            @Valid @RequestBody GoalRequest request
    ) {
        return dashboardService.updateGoal(goalId, request);
    }

    @DeleteMapping("/goals/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@PathVariable long goalId) {
        dashboardService.deleteGoal(goalId);
    }

    @PostMapping("/daily-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse.DailyTask createDailyTask(@Valid @RequestBody DailyTaskRequest request) {
        return dashboardService.createDailyTask(request);
    }

    @PutMapping("/daily-tasks/{taskId}")
    public DashboardResponse.DailyTask updateDailyTask(
            @PathVariable long taskId,
            @Valid @RequestBody DailyTaskRequest request
    ) {
        return dashboardService.updateDailyTask(taskId, request);
    }

    @DeleteMapping("/daily-tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDailyTask(@PathVariable long taskId) {
        dashboardService.deleteDailyTask(taskId);
    }

    @PostMapping("/weekly-quests")
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse.WeeklyQuest createWeeklyQuest(@Valid @RequestBody WeeklyQuestRequest request) {
        return dashboardService.createWeeklyQuest(request);
    }

    @PutMapping("/weekly-quests/{weeklyQuestId}")
    public DashboardResponse.WeeklyQuest updateWeeklyQuest(
            @PathVariable long weeklyQuestId,
            @Valid @RequestBody WeeklyQuestRequest request
    ) {
        return dashboardService.updateWeeklyQuest(weeklyQuestId, request);
    }

    @DeleteMapping("/weekly-quests/{weeklyQuestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWeeklyQuest(@PathVariable long weeklyQuestId) {
        dashboardService.deleteWeeklyQuest(weeklyQuestId);
    }

    @PostMapping("/goals/{goalId}/recommendations")
    public List<DashboardResponse.DailyTask> recommend(
            @PathVariable long goalId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return backendRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/goals/{goalId}/recommendations")
                        .queryParamIfPresent("taskDate", java.util.Optional.ofNullable(taskDate))
                        .build(goalId))
                .retrieve()
                .body(TASK_LIST);
    }

    @GetMapping("/goals/{goalId}/recommendations/preview")
    public List<DashboardResponse.RecommendationDraft> previewRecommendations(
            @PathVariable long goalId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return backendRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/be/goals/{goalId}/recommendations/preview")
                        .queryParamIfPresent("taskDate", java.util.Optional.ofNullable(taskDate))
                        .build(goalId))
                .retrieve()
                .body(RECOMMENDATION_DRAFT_LIST);
    }

    @PostMapping("/daily-tasks/{taskId}/complete")
    public DailyTaskCompletionResponse completeDailyTask(@PathVariable long taskId) {
        return backendRestClient.post()
                .uri("/api/be/daily-tasks/{taskId}/complete", taskId)
                .retrieve()
                .body(DailyTaskCompletionResponse.class);
    }

    @PostMapping("/weekly-quests/{weeklyQuestId}/complete")
    public DashboardResponse.WeeklyQuestCompletionResult completeWeeklyQuest(
            @PathVariable long weeklyQuestId
    ) {
        return dashboardService.completeWeeklyQuest(weeklyQuestId);
    }

    @PostMapping("/boss-raids/{bossRaidId}/attempts")
    public DashboardResponse.RaidAttemptResult attemptRaid(@PathVariable long bossRaidId) {
        return dashboardService.attemptRaid(bossRaidId);
    }

    public record GoalRequest(
            @NotBlank @Size(max = 200) String title,
            String description,
            String status,
            LocalDate targetDate,
            LocalDate taskDate
    ) {
    }

    public record DailyTaskRequest(
            Long goalId,
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull LocalDate taskDate,
            String status,
            @Min(1) @Max(1000) int xpReward
    ) {
    }

    public record DailyTaskCompletionResponse(
            long taskId,
            long completionId,
            int xpAwarded,
            long totalXp,
            int level,
            int strength,
            int vitality
    ) {
    }

    public record WeeklyQuestRequest(
            Long goalId,
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull LocalDate weekStartDate,
            String status,
            @Min(1) @Max(5000) int xpReward
    ) {
    }
}
