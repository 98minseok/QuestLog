package com.als98.questlog.be.goal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GoalProgressSummaryService {

    private final JdbcTemplate jdbcTemplate;

    public GoalProgressSummaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GoalProgressSummary> findAll(long userId) {
        return jdbcTemplate.query(
                """
                WITH quest_rollup AS (
                    SELECT
                        goal_id,
                        'DAILY' AS quest_type,
                        status,
                        xp_reward
                    FROM daily_tasks
                    WHERE user_id = ? AND goal_id IS NOT NULL
                    UNION ALL
                    SELECT
                        goal_id,
                        'WEEKLY' AS quest_type,
                        status,
                        xp_reward
                    FROM weekly_quests
                    WHERE user_id = ? AND goal_id IS NOT NULL
                )
                SELECT
                    g.id AS goal_id,
                    COALESCE(count(*) FILTER (WHERE q.quest_type = 'DAILY'), 0) AS daily_quest_count,
                    COALESCE(count(*) FILTER (WHERE q.quest_type = 'WEEKLY'), 0) AS weekly_quest_count,
                    COALESCE(count(*) FILTER (WHERE q.status = 'COMPLETED'), 0) AS completed_quest_count,
                    COALESCE(count(*) FILTER (WHERE q.status = 'PENDING'), 0) AS pending_quest_count,
                    COALESCE(count(*) FILTER (WHERE q.status = 'SKIPPED'), 0) AS skipped_quest_count,
                    COALESCE(sum(q.xp_reward) FILTER (WHERE q.status = 'COMPLETED'), 0) AS earned_xp,
                    COALESCE(sum(q.xp_reward), 0) AS available_xp
                FROM goals g
                LEFT JOIN quest_rollup q ON q.goal_id = g.id
                WHERE g.user_id = ?
                GROUP BY g.id
                ORDER BY g.created_at DESC, g.id DESC
                """,
                GoalProgressSummaryService::mapSummary,
                userId,
                userId,
                userId
        );
    }

    private static GoalProgressSummary mapSummary(ResultSet resultSet, int rowNumber)
            throws SQLException {
        int dailyQuestCount = resultSet.getInt("daily_quest_count");
        int weeklyQuestCount = resultSet.getInt("weekly_quest_count");
        int completedQuestCount = resultSet.getInt("completed_quest_count");
        int totalQuestCount = dailyQuestCount + weeklyQuestCount;
        return new GoalProgressSummary(
                resultSet.getLong("goal_id"),
                dailyQuestCount,
                weeklyQuestCount,
                completedQuestCount,
                resultSet.getInt("pending_quest_count"),
                resultSet.getInt("skipped_quest_count"),
                resultSet.getLong("earned_xp"),
                resultSet.getLong("available_xp"),
                totalQuestCount == 0 ? 0 : Math.round(completedQuestCount * 100f / totalQuestCount)
        );
    }
}
