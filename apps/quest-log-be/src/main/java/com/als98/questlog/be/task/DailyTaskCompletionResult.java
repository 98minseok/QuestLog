package com.als98.questlog.be.task;

public record DailyTaskCompletionResult(
        long taskId,
        long completionId,
        int xpAwarded,
        long totalXp,
        int level,
        int strength,
        int vitality
) {
}
