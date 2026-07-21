package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.goal.Goal;
import com.als98.questlog.be.goal.GoalService;
import com.als98.questlog.be.task.DailyTask;
import com.als98.questlog.be.task.DailyTaskService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockRecommendationService {

    private static final String PROVIDER = "deterministic-mock";
    private static final String SOURCE = "AI_RECOMMENDED";

    private final GoalService goalService;
    private final DailyTaskService dailyTaskService;
    private final RecommendationHistoryService historyService;

    public MockRecommendationService(
            GoalService goalService,
            DailyTaskService dailyTaskService,
            RecommendationHistoryService historyService
    ) {
        this.goalService = goalService;
        this.dailyTaskService = dailyTaskService;
        this.historyService = historyService;
    }

    @Transactional
    public List<DailyTask> recommend(long userId, long goalId, LocalDate taskDate) {
        Goal goal = goalService.find(userId, goalId);
        List<DailyTask> existing = dailyTaskService.findAll(userId, taskDate, goalId).stream()
                .filter(task -> SOURCE.equals(task.source()))
                .toList();
        if (!existing.isEmpty()) {
            return existing;
        }

        return buildDrafts(goal, taskDate).stream()
                .map(draft -> {
                    DailyTask task = dailyTaskService.create(
                        userId,
                        draft.goalId(),
                        draft.title(),
                        draft.description(),
                        draft.taskDate(),
                        draft.xpReward(),
                        draft.source()
                    );
                    historyService.record(userId, goalId, task.id(), PROVIDER, "ACCEPTED", draft);
                    return task;
                })
                .toList();
    }

    @Transactional
    public List<DailyTask> accept(long userId, long goalId, List<RecommendationDraft> drafts) {
        goalService.find(userId, goalId);
        return drafts.stream()
                .map(draft -> {
                    RecommendationDraft acceptedDraft = new RecommendationDraft(
                            goalId,
                            draft.title(),
                            draft.description(),
                            draft.taskDate(),
                            draft.xpReward(),
                            SOURCE
                    );
                    DailyTask task = dailyTaskService.create(
                        userId,
                        goalId,
                        acceptedDraft.title(),
                        acceptedDraft.description(),
                        acceptedDraft.taskDate(),
                        acceptedDraft.xpReward(),
                        acceptedDraft.source()
                    );
                    historyService.record(userId, goalId, task.id(), PROVIDER, "ACCEPTED", acceptedDraft);
                    return task;
                })
                .toList();
    }

    @Transactional
    public List<RecommendationDraft> preview(long userId, long goalId, LocalDate taskDate) {
        Goal goal = goalService.find(userId, goalId);
        List<RecommendationDraft> drafts = buildDrafts(goal, taskDate);
        drafts.forEach(draft -> historyService.record(userId, goalId, null, PROVIDER, "PREVIEWED", draft));
        return drafts;
    }

    public List<RecommendationHistory> findHistory(long userId, long goalId, int limit) {
        goalService.find(userId, goalId);
        return historyService.findRecent(userId, goalId, limit);
    }

    private List<RecommendationDraft> buildDrafts(Goal goal, LocalDate taskDate) {
        String goalTitle = goal.title();
        return List.of(
                new RecommendationDraft(
                        goal.id(),
                        "Plan the next step for " + goalTitle,
                        "Write one concrete outcome and the smallest action that advances it.",
                        taskDate,
                        10,
                        SOURCE
                ),
                new RecommendationDraft(
                        goal.id(),
                        "Focus on " + goalTitle + " for 25 minutes",
                        "Complete one uninterrupted focus session.",
                        taskDate,
                        20,
                        SOURCE
                ),
                new RecommendationDraft(
                        goal.id(),
                        "Review progress on " + goalTitle,
                        "Record what moved forward and choose tomorrow's first action.",
                        taskDate,
                        10,
                        SOURCE
                )
        );
    }
}
