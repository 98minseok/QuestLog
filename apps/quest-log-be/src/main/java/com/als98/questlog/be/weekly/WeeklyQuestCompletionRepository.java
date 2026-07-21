package com.als98.questlog.be.weekly;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class WeeklyQuestCompletionRepository {

    private final JdbcTemplate jdbcTemplate;

    WeeklyQuestCompletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<CompletableWeeklyQuest> markCompleted(long userId, long weeklyQuestId) {
        List<CompletableWeeklyQuest> quests = jdbcTemplate.query(
                """
                UPDATE weekly_quests
                SET status = 'COMPLETED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND user_id = ?
                  AND status = 'PENDING'
                RETURNING id, xp_reward
                """,
                (resultSet, rowNumber) -> new CompletableWeeklyQuest(
                        resultSet.getLong("id"),
                        resultSet.getInt("xp_reward")
                ),
                weeklyQuestId,
                userId
        );
        return quests.stream().findFirst();
    }

    Optional<String> findStatus(long userId, long weeklyQuestId) {
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM weekly_quests WHERE id = ? AND user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                weeklyQuestId,
                userId
        );
        return statuses.stream().findFirst();
    }

    long insertCompletion(long weeklyQuestId, int xpAwarded) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO weekly_quest_completions (weekly_quest_id, xp_awarded)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                weeklyQuestId,
                xpAwarded
        );
    }

    record CompletableWeeklyQuest(long id, int xpReward) {
    }
}
