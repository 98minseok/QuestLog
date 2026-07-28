package com.als98.questlog.be.weekly;

import com.als98.questlog.be.api.ResourceNotFoundException;
import com.als98.questlog.be.goal.Goal;
import com.als98.questlog.be.goal.GoalService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyQuestService {

    private static final List<String> STATUSES = List.of("PENDING", "COMPLETED", "SKIPPED");

    private final GoalService goalService;
    private final JdbcTemplate jdbcTemplate;

    public WeeklyQuestService(GoalService goalService, JdbcTemplate jdbcTemplate) {
        this.goalService = goalService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WeeklyQuest> findAll(long userId, LocalDate weekStartDate, Long goalId) {
        return jdbcTemplate.query(
                """
                SELECT * FROM weekly_quests
                WHERE user_id = ?
                  AND (?::date IS NULL OR week_start_date = ?)
                  AND (?::bigint IS NULL OR goal_id = ?)
                ORDER BY week_start_date, created_at
                """,
                WeeklyQuestService::mapQuest,
                userId,
                weekStartDate,
                weekStartDate,
                goalId,
                goalId
        );
    }

    public WeeklyQuest find(long userId, long weeklyQuestId) {
        return jdbcTemplate.query(
                "SELECT * FROM weekly_quests WHERE id = ? AND user_id = ?",
                WeeklyQuestService::mapQuest,
                weeklyQuestId,
                userId
        ).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Weekly quest", weeklyQuestId));
    }

    @Transactional
    public List<WeeklyQuest> recommendForGoal(long userId, long goalId, LocalDate weekStartDate) {
        Goal goal = goalService.find(userId, goalId);
        List<WeeklyQuest> existing = findSystemWeeklyQuests(userId, goalId, weekStartDate);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<WeeklyQuestDraft> drafts = List.of(
                new WeeklyQuestDraft(
                        trimToMaxLength("Define the weekly milestone for " + goal.title(), 200),
                        "Choose one measurable outcome that would make this week successful.",
                        75
                ),
                new WeeklyQuestDraft(
                        trimToMaxLength("Review progress and risks for " + goal.title(), 200),
                        "Summarize completed work, blockers, and the next concrete adjustment.",
                        60
                )
        );
        drafts.forEach(draft -> create(
                userId,
                goalId,
                draft.title(),
                draft.description(),
                weekStartDate,
                draft.xpReward(),
                "SYSTEM"
        ));
        return findSystemWeeklyQuests(userId, goalId, weekStartDate);
    }

    @Transactional
    public WeeklyQuest create(
            long userId,
            Long goalId,
            String title,
            String description,
            LocalDate weekStartDate,
            int xpReward,
            String source
    ) {
        validateGoalOwnership(userId, goalId);
        long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO weekly_quests (
                    user_id, goal_id, title, description, week_start_date, xp_reward, source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                goalId,
                title.trim(),
                blankToNull(description),
                weekStartDate,
                xpReward,
                source
        );
        return find(userId, id);
    }

    @Transactional
    public WeeklyQuest update(
            long userId,
            long weeklyQuestId,
            Long goalId,
            String title,
            String description,
            LocalDate weekStartDate,
            String status,
            int xpReward
    ) {
        WeeklyQuest currentQuest = find(userId, weeklyQuestId);
        String normalizedStatus = status == null
                ? currentQuest.status()
                : status.toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported weekly quest status: " + status);
        }
        requirePendingForEditing(currentQuest);
        if ("COMPLETED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Use the weekly quest completion endpoint to complete a quest");
        }
        validateGoalOwnership(userId, goalId);
        int updated = jdbcTemplate.update(
                """
                UPDATE weekly_quests
                SET goal_id = ?, title = ?, description = ?, week_start_date = ?, status = ?,
                    xp_reward = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                goalId,
                title.trim(),
                blankToNull(description),
                weekStartDate,
                normalizedStatus,
                xpReward,
                weeklyQuestId,
                userId
        );
        if (updated == 0) {
            throw new ResourceNotFoundException("Weekly quest", weeklyQuestId);
        }
        return find(userId, weeklyQuestId);
    }

    @Transactional
    public void delete(long userId, long weeklyQuestId) {
        WeeklyQuest currentQuest = find(userId, weeklyQuestId);
        if (!"PENDING".equals(currentQuest.status())) {
            throw new WeeklyQuestNotDeletableException(weeklyQuestId, currentQuest.status());
        }
        if (jdbcTemplate.update(
                "DELETE FROM weekly_quests WHERE id = ? AND user_id = ?",
                weeklyQuestId,
                userId
        ) == 0) {
            throw new ResourceNotFoundException("Weekly quest", weeklyQuestId);
        }
    }

    private static void requirePendingForEditing(WeeklyQuest currentQuest) {
        if ("PENDING".equals(currentQuest.status())) {
            return;
        }
        String displayStatus = currentQuest.status().substring(0, 1)
                + currentQuest.status().substring(1).toLowerCase(Locale.ROOT);
        throw new IllegalArgumentException(displayStatus + " weekly quests cannot be edited");
    }

    private List<WeeklyQuest> findSystemWeeklyQuests(long userId, long goalId, LocalDate weekStartDate) {
        return jdbcTemplate.query(
                """
                SELECT * FROM weekly_quests
                WHERE user_id = ?
                  AND goal_id = ?
                  AND week_start_date = ?
                  AND source = 'SYSTEM'
                ORDER BY created_at, id
                """,
                WeeklyQuestService::mapQuest,
                userId,
                goalId,
                weekStartDate
        );
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

    static WeeklyQuest mapQuest(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WeeklyQuest(
                resultSet.getLong("id"),
                resultSet.getObject("goal_id", Long.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getObject("week_start_date", LocalDate.class),
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

    private static String trimToMaxLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record WeeklyQuestDraft(String title, String description, int xpReward) {
    }
}
