package com.als98.questlog.be.raid;

public record RaidAttemptResult(
        long attemptId,
        long bossRaidId,
        String bossName,
        int stage,
        String status,
        int damageDealt,
        int bossRemainingHp,
        int xpAwarded,
        long totalXp,
        int level,
        int strength,
        int vitality
) {
}
