package com.als98.questlog.be.recommendation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RecommendationHistory record(
            long userId,
            long goalId,
            Long createdTaskId,
            String provider,
            String action,
            RecommendationDraft draft
    ) {
        long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO recommendation_history (
                    user_id, goal_id, created_task_id, provider, action, title,
                    description, task_date, xp_reward, source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                goalId,
                createdTaskId,
                provider,
                action,
                draft.title().trim(),
                blankToNull(draft.description()),
                draft.taskDate(),
                draft.xpReward(),
                draft.source()
        );
        return find(userId, id);
    }

    public List<RecommendationHistory> findRecent(long userId, long goalId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT * FROM recommendation_history
                WHERE user_id = ? AND goal_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                RecommendationHistoryService::mapHistory,
                userId,
                goalId,
                limit
        );
    }

    private RecommendationHistory find(long userId, long id) {
        return jdbcTemplate.query(
                "SELECT * FROM recommendation_history WHERE id = ? AND user_id = ?",
                RecommendationHistoryService::mapHistory,
                id,
                userId
        ).getFirst();
    }

    private static RecommendationHistory mapHistory(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new RecommendationHistory(
                resultSet.getLong("id"),
                resultSet.getLong("goal_id"),
                resultSet.getObject("created_task_id", Long.class),
                resultSet.getString("provider"),
                resultSet.getString("action"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getObject("task_date", java.time.LocalDate.class),
                resultSet.getInt("xp_reward"),
                resultSet.getString("source"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
