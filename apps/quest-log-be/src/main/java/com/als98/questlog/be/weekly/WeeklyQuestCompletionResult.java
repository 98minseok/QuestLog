package com.als98.questlog.be.weekly;

public record WeeklyQuestCompletionResult(
        long weeklyQuestId,
        long completionId,
        int xpAwarded,
        long totalXp,
        int level,
        int strength,
        int vitality
) {
}
