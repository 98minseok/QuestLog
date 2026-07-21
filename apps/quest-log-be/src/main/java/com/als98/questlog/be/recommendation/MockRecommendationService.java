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

    private final GoalService goalService;
    private final DailyTaskService dailyTaskService;

    public MockRecommendationService(GoalService goalService, DailyTaskService dailyTaskService) {
        this.goalService = goalService;
        this.dailyTaskService = dailyTaskService;
    }

    @Transactional
    public List<DailyTask> recommend(long userId, long goalId, LocalDate taskDate) {
        Goal goal = goalService.find(userId, goalId);
        List<DailyTask> existing = dailyTaskService.findAll(userId, taskDate, goalId).stream()
                .filter(task -> "AI_RECOMMENDED".equals(task.source()))
                .toList();
        if (!existing.isEmpty()) {
            return existing;
        }

        return preview(userId, goalId, taskDate).stream()
                .map(draft -> dailyTaskService.create(
                        userId,
                        draft.goalId(),
                        draft.title(),
                        draft.description(),
                        draft.taskDate(),
                        draft.xpReward(),
                        draft.source()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationDraft> preview(long userId, long goalId, LocalDate taskDate) {
        Goal goal = goalService.find(userId, goalId);
        String goalTitle = goal.title();
        return List.of(
                new RecommendationDraft(
                        goalId,
                        "Plan the next step for " + goalTitle,
                        "Write one concrete outcome and the smallest action that advances it.",
                        taskDate,
                        10,
                        "AI_RECOMMENDED"
                ),
                new RecommendationDraft(
                        goalId,
                        "Focus on " + goalTitle + " for 25 minutes",
                        "Complete one uninterrupted focus session.",
                        taskDate,
                        20,
                        "AI_RECOMMENDED"
                ),
                new RecommendationDraft(
                        goalId,
                        "Review progress on " + goalTitle,
                        "Record what moved forward and choose tomorrow's first action.",
                        taskDate,
                        10,
                        "AI_RECOMMENDED"
                )
        );
    }
}
