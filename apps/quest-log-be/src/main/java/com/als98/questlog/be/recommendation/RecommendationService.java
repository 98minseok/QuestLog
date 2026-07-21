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
public class RecommendationService {

    private static final String SOURCE = "AI_RECOMMENDED";

    private final GoalService goalService;
    private final DailyTaskService dailyTaskService;
    private final RecommendationHistoryService historyService;
    private final RecommendationProvider recommendationProvider;

    public RecommendationService(
            GoalService goalService,
            DailyTaskService dailyTaskService,
            RecommendationHistoryService historyService,
            RecommendationProvider recommendationProvider
    ) {
        this.goalService = goalService;
        this.dailyTaskService = dailyTaskService;
        this.historyService = historyService;
        this.recommendationProvider = recommendationProvider;
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

        return recommendationProvider.dailyTaskDrafts(goal, taskDate).stream()
                .map(draft -> createRecommendedTask(userId, goalId, draft))
                .toList();
    }

    @Transactional
    public List<DailyTask> accept(long userId, long goalId, List<RecommendationDraft> drafts) {
        goalService.find(userId, goalId);
        return drafts.stream()
                .map(draft -> new RecommendationDraft(
                        goalId,
                        draft.title(),
                        draft.description(),
                        draft.taskDate(),
                        draft.xpReward(),
                        SOURCE
                ))
                .map(draft -> createRecommendedTask(userId, goalId, draft))
                .toList();
    }

    @Transactional
    public List<RecommendationDraft> preview(long userId, long goalId, LocalDate taskDate) {
        Goal goal = goalService.find(userId, goalId);
        List<RecommendationDraft> drafts = recommendationProvider.dailyTaskDrafts(goal, taskDate);
        drafts.forEach(draft -> historyService.record(
                userId,
                goalId,
                null,
                recommendationProvider.providerId(),
                "PREVIEWED",
                draft
        ));
        return drafts;
    }

    public List<RecommendationHistory> findHistory(long userId, long goalId, int limit) {
        goalService.find(userId, goalId);
        return historyService.findRecent(userId, goalId, limit);
    }

    private DailyTask createRecommendedTask(long userId, long goalId, RecommendationDraft draft) {
        DailyTask task = dailyTaskService.create(
                userId,
                goalId,
                draft.title(),
                draft.description(),
                draft.taskDate(),
                draft.xpReward(),
                SOURCE
        );
        historyService.record(
                userId,
                goalId,
                task.id(),
                recommendationProvider.providerId(),
                "ACCEPTED",
                new RecommendationDraft(
                        goalId,
                        draft.title(),
                        draft.description(),
                        draft.taskDate(),
                        draft.xpReward(),
                        SOURCE
                )
        );
        return task;
    }
}
