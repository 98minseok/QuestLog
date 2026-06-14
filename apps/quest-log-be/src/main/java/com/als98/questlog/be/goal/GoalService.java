package com.als98.questlog.be.goal;

import com.als98.questlog.be.api.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private static final List<String> STATUSES = List.of("ACTIVE", "COMPLETED", "ARCHIVED");

    private final JdbcTemplate jdbcTemplate;

    public GoalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Goal> findAll(long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM goals WHERE user_id = ? ORDER BY created_at DESC",
                GoalService::mapGoal,
                userId
        );
    }

    public Goal find(long userId, long goalId) {
        return jdbcTemplate.query(
                "SELECT * FROM goals WHERE id = ? AND user_id = ?",
                GoalService::mapGoal,
                goalId,
                userId
        ).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
    }

    @Transactional
    public Goal create(long userId, String title, String description, LocalDate targetDate) {
        long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO goals (user_id, title, description, target_date)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                title.trim(),
                blankToNull(description),
                targetDate
        );
        return find(userId, id);
    }

    @Transactional
    public Goal update(
            long userId,
            long goalId,
            String title,
            String description,
            String status,
            LocalDate targetDate
    ) {
        String normalizedStatus = status.toUpperCase();
        if (!STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported goal status: " + status);
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE goals
                SET title = ?, description = ?, status = ?, target_date = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                title.trim(),
                blankToNull(description),
                normalizedStatus,
                targetDate,
                goalId,
                userId
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("Goal", goalId);
        }
        return find(userId, goalId);
    }

    @Transactional
    public void delete(long userId, long goalId) {
        if (jdbcTemplate.update("DELETE FROM goals WHERE id = ? AND user_id = ?", goalId, userId) == 0) {
            throw new ResourceNotFoundException("Goal", goalId);
        }
    }

    private static Goal mapGoal(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Goal(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                resultSet.getObject("target_date", LocalDate.class),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
