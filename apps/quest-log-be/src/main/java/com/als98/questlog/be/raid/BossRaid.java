package com.als98.questlog.be.raid;

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
