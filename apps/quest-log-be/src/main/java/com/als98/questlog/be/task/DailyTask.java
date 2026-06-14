package com.als98.questlog.be.task;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
