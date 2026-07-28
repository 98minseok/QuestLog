package com.als98.questlog.be.progression;

import java.time.OffsetDateTime;

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
