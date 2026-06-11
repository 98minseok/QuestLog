package com.als98.questlog.be.task;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class DailyTaskCompletionRepository {

    private final JdbcTemplate jdbcTemplate;

    DailyTaskCompletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<CompletableTask> markCompleted(long userId, long taskId) {
        List<CompletableTask> tasks = jdbcTemplate.query(
                """
                UPDATE daily_tasks
                SET status = 'COMPLETED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND user_id = ?
                  AND status = 'PENDING'
                RETURNING id, xp_reward
                """,
                (resultSet, rowNumber) -> new CompletableTask(
                        resultSet.getLong("id"),
                        resultSet.getInt("xp_reward")
                ),
                taskId,
                userId
        );
        return tasks.stream().findFirst();
    }

    Optional<String> findStatus(long userId, long taskId) {
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM daily_tasks WHERE id = ? AND user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                taskId,
                userId
        );
        return statuses.stream().findFirst();
    }

    long insertCompletion(long taskId, int xpAwarded) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO task_completions (task_id, xp_awarded)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                taskId,
                xpAwarded
        );
    }

    CharacterProgression addExperience(long userId, int xpAwarded) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO character_profiles (
                    user_id,
                    total_xp,
                    level,
                    strength,
                    vitality
                )
                VALUES (
                    ?,
                    ?,
                    (? / 100) + 1,
                    (? / 100) + 1,
                    (? / 100) + 1
                )
                ON CONFLICT (user_id) DO UPDATE
                SET total_xp = character_profiles.total_xp + EXCLUDED.total_xp,
                    level = ((character_profiles.total_xp + EXCLUDED.total_xp) / 100 + 1)::integer,
                    strength = ((character_profiles.total_xp + EXCLUDED.total_xp) / 100 + 1)::integer,
                    vitality = ((character_profiles.total_xp + EXCLUDED.total_xp) / 100 + 1)::integer,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING total_xp, level, strength, vitality
                """,
                (resultSet, rowNumber) -> new CharacterProgression(
                        resultSet.getLong("total_xp"),
                        resultSet.getInt("level"),
                        resultSet.getInt("strength"),
                        resultSet.getInt("vitality")
                ),
                userId,
                xpAwarded,
                xpAwarded,
                xpAwarded,
                xpAwarded
        );
    }

    record CompletableTask(long id, int xpReward) {
    }

    record CharacterProgression(long totalXp, int level, int strength, int vitality) {
    }
}
