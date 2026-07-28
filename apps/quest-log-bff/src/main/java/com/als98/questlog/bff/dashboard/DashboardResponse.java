package com.als98.questlog.bff.dashboard;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDate taskDate,
        List<Goal> goals,
        List<DailyTask> dailyTasks,
        List<WeeklyQuest> weeklyQuests,
        CharacterProfile character,
        List<CharacterProgressionEvent> progressionEvents,
        List<BossRaid> raids,
        List<RaidAttempt> raidAttempts
) {

    public record Goal(
            long id,
            String title,
            String description,
            String status,
            LocalDate targetDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record DailyTask(
            long id,
            Long goalId,
            String title,
            String description,
            LocalDate taskDate,
            String status,
            String source,
            int xpReward,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record RecommendationDraft(
            Long goalId,
            String title,
            String description,
            LocalDate taskDate,
            int xpReward,
            String source
    ) {
    }

    public record RecommendationHistory(
            long id,
            long goalId,
            Long createdTaskId,
            String provider,
            String action,
            String title,
            String description,
            LocalDate taskDate,
            int xpReward,
            String source,
            OffsetDateTime createdAt
    ) {
    }

    public record WeeklyQuest(
            long id,
            Long goalId,
            String title,
            String description,
            LocalDate weekStartDate,
            String status,
            String source,
            int xpReward,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CharacterProfile(
            long userId,
            String displayName,
            int level,
            long totalXp,
            long currentLevelXp,
            long xpToNextLevel,
            int strength,
            int vitality
    ) {
    }

    public record CharacterProgressionEvent(
            long id,
            long userId,
            String sourceType,
            long sourceId,
            int xpAwarded,
            long totalXp,
            int level,
            int strength,
            int vitality,
            OffsetDateTime createdAt
    ) {
    }

    public record BossRaid(
            long id,
            int stage,
            String name,
            int requiredLevel,
            int maxHp,
            int xpReward,
            boolean active,
            boolean unlocked
    ) {
    }

    public record RaidAttempt(
            long id,
            long bossRaidId,
            String bossName,
            int stage,
            String status,
            int damageDealt,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
    }

    public record RaidAttemptResult(
            long attemptId,
            long bossRaidId,
            String bossName,
            int stage,
            String status,
            int damageDealt,
            int xpAwarded,
            long totalXp,
            int level,
            int strength,
            int vitality
    ) {
    }

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
}
