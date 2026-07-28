package com.als98.questlog.be.goal;

public record GoalProgressSummary(
        long goalId,
        int dailyQuestCount,
        int weeklyQuestCount,
        int completedQuestCount,
        int pendingQuestCount,
        int skippedQuestCount,
        long earnedXp,
        long availableXp,
        int completionRate
) {
}
