package com.als98.questlog.be.progression;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CharacterProgressionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CharacterProgressionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CharacterProgression addExperience(long userId, int xpAwarded) {
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

    public record CharacterProgression(long totalXp, int level, int strength, int vitality) {
    }
}
