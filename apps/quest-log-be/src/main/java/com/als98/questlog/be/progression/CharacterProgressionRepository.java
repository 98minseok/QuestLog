package com.als98.questlog.be.progression;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CharacterProgressionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CharacterProgressionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CharacterProgression addExperience(
            long userId,
            int xpAwarded,
            ProgressionSourceType sourceType,
            long sourceId
    ) {
        CharacterProgression progression = jdbcTemplate.queryForObject(
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
        jdbcTemplate.update(
                """
                INSERT INTO character_progression_events (
                    user_id,
                    source_type,
                    source_id,
                    xp_awarded,
                    total_xp,
                    level,
                    strength,
                    vitality
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                sourceType.name(),
                sourceId,
                xpAwarded,
                progression.totalXp(),
                progression.level(),
                progression.strength(),
                progression.vitality()
        );
        return progression;
    }

    public List<CharacterProgressionEvent> findRecentEvents(long userId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, user_id, source_type, source_id, xp_awarded,
                       total_xp, level, strength, vitality, created_at
                FROM character_progression_events
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                (resultSet, rowNumber) -> new CharacterProgressionEvent(
                        resultSet.getLong("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("source_type"),
                        resultSet.getLong("source_id"),
                        resultSet.getInt("xp_awarded"),
                        resultSet.getLong("total_xp"),
                        resultSet.getInt("level"),
                        resultSet.getInt("strength"),
                        resultSet.getInt("vitality"),
                        resultSet.getObject("created_at", java.time.OffsetDateTime.class)
                ),
                userId,
                limit
        );
    }

    public record CharacterProgression(long totalXp, int level, int strength, int vitality) {
    }
}
