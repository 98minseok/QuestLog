package com.als98.questlog.be.raid;

import com.als98.questlog.be.api.ResourceNotFoundException;
import com.als98.questlog.be.progression.CharacterProgressionRepository;
import com.als98.questlog.be.progression.CharacterProgressionRepository.CharacterProgression;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaidService {

    private final JdbcTemplate jdbcTemplate;
    private final CharacterProgressionRepository progressionRepository;

    public RaidService(
            JdbcTemplate jdbcTemplate,
            CharacterProgressionRepository progressionRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.progressionRepository = progressionRepository;
    }

    @Transactional
    public RaidAttemptResult attempt(long userId, long bossRaidId) {
        RaidTarget raid = findRaid(bossRaidId);
        if (!raid.active()) {
            throw new ResourceNotFoundException("Boss raid", bossRaidId);
        }
        if (hasVictory(userId, bossRaidId)) {
            throw new RaidAlreadyClearedException(bossRaidId);
        }

        int level = currentLevel(userId);
        if (level < raid.requiredLevel()) {
            throw new RaidLockedException(bossRaidId, raid.requiredLevel());
        }

        long attemptId = jdbcTemplate.queryForObject(
                """
                INSERT INTO raid_attempts (
                    user_id,
                    boss_raid_id,
                    status,
                    damage_dealt,
                    completed_at
                )
                VALUES (?, ?, 'VICTORY', ?, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                userId,
                bossRaidId,
                raid.maxHp()
        );
        CharacterProgression progression =
                progressionRepository.addExperience(userId, raid.xpReward());

        return new RaidAttemptResult(
                attemptId,
                bossRaidId,
                raid.name(),
                raid.stage(),
                "VICTORY",
                raid.maxHp(),
                raid.xpReward(),
                progression.totalXp(),
                progression.level(),
                progression.strength(),
                progression.vitality()
        );
    }

    private RaidTarget findRaid(long bossRaidId) {
        List<RaidTarget> raids = jdbcTemplate.query(
                """
                SELECT id, stage, name, required_level, max_hp, xp_reward, active
                FROM boss_raids
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> new RaidTarget(
                        resultSet.getLong("id"),
                        resultSet.getInt("stage"),
                        resultSet.getString("name"),
                        resultSet.getInt("required_level"),
                        resultSet.getInt("max_hp"),
                        resultSet.getInt("xp_reward"),
                        resultSet.getBoolean("active")
                ),
                bossRaidId
        );
        return raids.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Boss raid", bossRaidId));
    }

    private boolean hasVictory(long userId, long bossRaidId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM raid_attempts
                    WHERE user_id = ?
                      AND boss_raid_id = ?
                      AND status = 'VICTORY'
                )
                """,
                Boolean.class,
                userId,
                bossRaidId
        ));
    }

    private int currentLevel(long userId) {
        Integer level = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(level), 1) FROM character_profiles WHERE user_id = ?",
                Integer.class,
                userId
        );
        return level == null ? 1 : level;
    }

    private record RaidTarget(
            long id,
            int stage,
            String name,
            int requiredLevel,
            int maxHp,
            int xpReward,
            boolean active
    ) {
    }
}
