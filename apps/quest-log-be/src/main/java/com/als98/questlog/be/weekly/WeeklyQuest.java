package com.als98.questlog.be.weekly;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
