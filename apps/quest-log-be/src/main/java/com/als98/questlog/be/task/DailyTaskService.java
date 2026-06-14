package com.als98.questlog.be.task;

import com.als98.questlog.be.api.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyTaskService {

    private static final List<String> STATUSES = List.of("PENDING", "COMPLETED", "SKIPPED");

    private final JdbcTemplate jdbcTemplate;

    public DailyTaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DailyTask> findAll(long userId, LocalDate taskDate, Long goalId) {
        return jdbcTemplate.query(
                """
                SELECT * FROM daily_tasks
                WHERE user_id = ?
                  AND (?::date IS NULL OR task_date = ?)
                  AND (?::bigint IS NULL OR goal_id = ?)
                ORDER BY task_date, created_at
                """,
                DailyTaskService::mapTask,
                userId,
                taskDate,
                taskDate,
                goalId,
                goalId
        );
    }

    public DailyTask find(long userId, long taskId) {
        return jdbcTemplate.query(
                "SELECT * FROM daily_tasks WHERE id = ? AND user_id = ?",
                DailyTaskService::mapTask,
                taskId,
                userId
        ).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Daily task", taskId));
    }

    @Transactional
    public DailyTask create(
            long userId,
            Long goalId,
            String title,
            String description,
            LocalDate taskDate,
            int xpReward,
            String source
    ) {
        validateGoalOwnership(userId, goalId);
        long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO daily_tasks (
                    user_id, goal_id, title, description, task_date, xp_reward, source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                goalId,
                title.trim(),
                blankToNull(description),
                taskDate,
                xpReward,
                source
        );
        return find(userId, id);
    }

    @Transactional
    public DailyTask update(
            long userId,
            long taskId,
            Long goalId,
            String title,
            String description,
            LocalDate taskDate,
            String status,
            int xpReward
    ) {
        DailyTask currentTask = find(userId, taskId);
        String normalizedStatus = status.toUpperCase();
        if (!STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported daily task status: " + status);
        }
        requirePendingForEditing(currentTask);
        if ("COMPLETED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Use the daily task completion endpoint to complete a task");
        }
        validateGoalOwnership(userId, goalId);
        int updated = jdbcTemplate.update(
                """
                UPDATE daily_tasks
                SET goal_id = ?, title = ?, description = ?, task_date = ?, status = ?,
                    xp_reward = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                goalId,
                title.trim(),
                blankToNull(description),
                taskDate,
                normalizedStatus,
                xpReward,
                taskId,
                userId
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("Daily task", taskId);
        }
        return find(userId, taskId);
    }

    private static void requirePendingForEditing(DailyTask currentTask) {
        if ("PENDING".equals(currentTask.status())) {
            return;
        }
        String displayStatus = currentTask.status().substring(0, 1)
                + currentTask.status().substring(1).toLowerCase(Locale.ROOT);
        throw new IllegalArgumentException(displayStatus + " daily tasks cannot be edited");
    }

    @Transactional
    public void delete(long userId, long taskId) {
        requirePendingForDeletion(userId, taskId);
        if (jdbcTemplate.update("DELETE FROM daily_tasks WHERE id = ? AND user_id = ?", taskId, userId) == 0) {
            throw new ResourceNotFoundException("Daily task", taskId);
        }
    }

    private void requirePendingForDeletion(long userId, long taskId) {
        DailyTask currentTask = find(userId, taskId);
        if (!"PENDING".equals(currentTask.status())) {
            throw new DailyTaskNotDeletableException(taskId, currentTask.status());
        }
    }

    private void validateGoalOwnership(long userId, Long goalId) {
        if (goalId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM goals WHERE id = ? AND user_id = ?",
                Integer.class,
                goalId,
                userId
        );
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Goal", goalId);
        }
    }

    static DailyTask mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DailyTask(
                resultSet.getLong("id"),
                resultSet.getObject("goal_id", Long.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getObject("task_date", LocalDate.class),
                resultSet.getString("status"),
                resultSet.getString("source"),
                resultSet.getInt("xp_reward"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
