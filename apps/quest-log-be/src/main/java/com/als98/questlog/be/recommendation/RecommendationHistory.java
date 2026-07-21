package com.als98.questlog.be.recommendation;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
