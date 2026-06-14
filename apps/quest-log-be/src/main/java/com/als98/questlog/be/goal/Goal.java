package com.als98.questlog.be.goal;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
