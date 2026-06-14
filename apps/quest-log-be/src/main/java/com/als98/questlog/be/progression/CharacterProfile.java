package com.als98.questlog.be.progression;

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
