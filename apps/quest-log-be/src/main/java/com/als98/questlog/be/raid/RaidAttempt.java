package com.als98.questlog.be.raid;

import java.time.OffsetDateTime;

public record RaidAttempt(
        long id,
        long bossRaidId,
        String bossName,
        int stage,
        String status,
        int damageDealt,
        int bossRemainingHp,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}
