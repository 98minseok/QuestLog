package com.als98.questlog.bff.dashboard;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDate taskDate,
        List<Goal> goals,
        List<DailyTask> dailyTasks,
        CharacterProfile character,
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
}
