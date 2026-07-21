package com.als98.questlog.be.recommendation;

import java.time.LocalDate;

public record RecommendationDraft(
        Long goalId,
        String title,
        String description,
        LocalDate taskDate,
        int xpReward,
        String source
) {
}
