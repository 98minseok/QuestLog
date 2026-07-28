package com.als98.questlog.be.raid;

import com.als98.questlog.be.api.ResourceNotFoundException;
import com.als98.questlog.be.progression.CharacterProgressionRepository;
import com.als98.questlog.be.progression.CharacterProgressionRepository.CharacterProgression;
import com.als98.questlog.be.progression.ProgressionSourceType;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaidService {

    private final JdbcTemplate jdbcTemplate;
    private final CharacterProgressionRepository progressionRepository;
    private final RaidCombatPolicy combatPolicy;

    public RaidService(
            JdbcTemplate jdbcTemplate,
            CharacterProgressionRepository progressionRepository,
            RaidCombatPolicy combatPolicy
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.progressionRepository = progressionRepository;
        this.combatPolicy = combatPolicy;
    }

    @Transactional
    public RaidAttemptResult attempt(long userId, long bossRaidId) {
        RaidAttempt activeAttempt = startAttempt(userId, bossRaidId);
        RaidAttemptResult result;
        do {
            result = attack(userId, activeAttempt.id());
        } while ("IN_PROGRESS".equals(result.status()));
        return result;
    }

    @Transactional
    public RaidAttempt startAttempt(long userId, long bossRaidId) {
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

        List<RaidAttempt> activeAttempts = findActiveAttempt(userId, bossRaidId);
        if (!activeAttempts.isEmpty()) {
            return activeAttempts.get(0);
        }

        long attemptId = jdbcTemplate.queryForObject(
                """
                INSERT INTO raid_attempts (
                    user_id,
                    boss_raid_id,
                    status,
                    damage_dealt,
                    boss_remaining_hp
                )
                VALUES (?, ?, 'STARTED', 0, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                bossRaidId,
                raid.maxHp()
        );
        return findActiveAttempt(userId, bossRaidId).stream()
                .filter(attempt -> attempt.id() == attemptId)
                .findFirst()
                .orElseThrow(() -> new RaidAttemptNotFoundException(attemptId));
    }

    @Transactional
    public RaidAttemptResult attack(long userId, long raidAttemptId) {
        RaidAttemptState attempt = findAttempt(userId, raidAttemptId);
        if (!"STARTED".equals(attempt.status()) && !"IN_PROGRESS".equals(attempt.status())) {
            throw new RaidAttemptNotActiveException(raidAttemptId, attempt.status());
        }

        CharacterStats stats = currentStats(userId);
        RaidCombatPolicy.RaidAttackOutcome outcome = combatPolicy.attack(
                stats.level(),
                stats.strength(),
                stats.vitality(),
                attempt.damageDealt(),
                attempt.bossRemainingHp()
        );
        jdbcTemplate.update(
                """
                UPDATE raid_attempts
                SET status = ?,
                    damage_dealt = ?,
                    boss_remaining_hp = ?,
                    completed_at = CASE WHEN ? = 'CLEARED' THEN CURRENT_TIMESTAMP ELSE NULL END
                WHERE id = ?
                  AND user_id = ?
                """,
                outcome.status(),
                outcome.totalDamage(),
                outcome.bossRemainingHp(),
                outcome.status(),
                raidAttemptId,
                userId
        );

        if ("CLEARED".equals(outcome.status())) {
            CharacterProgression progression =
                    progressionRepository.addExperience(
                            userId,
                            attempt.xpReward(),
                            ProgressionSourceType.BOSS_RAID,
                            attempt.bossRaidId()
                    );
            return result(
                    attempt,
                    outcome.status(),
                    outcome.totalDamage(),
                    outcome.bossRemainingHp(),
                    attempt.xpReward(),
                    progression.totalXp(),
                    progression.level(),
                    progression.strength(),
                    progression.vitality()
            );
        }

        return result(
                attempt,
                outcome.status(),
                outcome.totalDamage(),
                outcome.bossRemainingHp(),
                0,
                stats.totalXp(),
                stats.level(),
                stats.strength(),
                stats.vitality()
        );
    }

    @Transactional
    public RaidAttemptResult resolve(long userId, long raidAttemptId) {
        RaidAttemptState attempt = findAttempt(userId, raidAttemptId);
        if (!"STARTED".equals(attempt.status()) && !"IN_PROGRESS".equals(attempt.status())) {
            throw new RaidAttemptNotActiveException(raidAttemptId, attempt.status());
        }

        CharacterStats stats = currentStats(userId);
        jdbcTemplate.update(
                """
                UPDATE raid_attempts
                SET status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND user_id = ?
                """,
                raidAttemptId,
                userId
        );

        return result(
                attempt,
                "FAILED",
                attempt.damageDealt(),
                attempt.bossRemainingHp(),
                0,
                stats.totalXp(),
                stats.level(),
                stats.strength(),
                stats.vitality()
        );
    }

    private RaidAttemptResult result(
            RaidAttemptState attempt,
            String status,
            int damageDealt,
            int bossRemainingHp,
            int xpAwarded,
            long totalXp,
            int level,
            int strength,
            int vitality
    ) {
        return new RaidAttemptResult(
                attempt.id(),
                attempt.bossRaidId(),
                attempt.bossName(),
                attempt.stage(),
                status,
                damageDealt,
                bossRemainingHp,
                xpAwarded,
                totalXp,
                level,
                strength,
                vitality
        );
    }

    private List<RaidAttempt> findActiveAttempt(long userId, long bossRaidId) {
        return jdbcTemplate.query(
                """
                SELECT a.*, b.name AS boss_name, b.stage
                FROM raid_attempts a
                JOIN boss_raids b ON b.id = a.boss_raid_id
                WHERE a.user_id = ?
                  AND a.boss_raid_id = ?
                  AND a.status IN ('STARTED', 'IN_PROGRESS')
                ORDER BY a.started_at DESC
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new RaidAttempt(
                        resultSet.getLong("id"),
                        resultSet.getLong("boss_raid_id"),
                        resultSet.getString("boss_name"),
                        resultSet.getInt("stage"),
                        resultSet.getString("status"),
                        resultSet.getInt("damage_dealt"),
                        resultSet.getInt("boss_remaining_hp"),
                        resultSet.getObject("started_at", java.time.OffsetDateTime.class),
                        resultSet.getObject("completed_at", java.time.OffsetDateTime.class)
                ),
                userId,
                bossRaidId
        );
    }

    private RaidAttemptState findAttempt(long userId, long raidAttemptId) {
        List<RaidAttemptState> attempts = jdbcTemplate.query(
                """
                SELECT a.*, b.name AS boss_name, b.stage, b.xp_reward
                FROM raid_attempts a
                JOIN boss_raids b ON b.id = a.boss_raid_id
                WHERE a.id = ?
                  AND a.user_id = ?
                """,
                (resultSet, rowNumber) -> new RaidAttemptState(
                        resultSet.getLong("id"),
                        resultSet.getLong("boss_raid_id"),
                        resultSet.getString("boss_name"),
                        resultSet.getInt("stage"),
                        resultSet.getString("status"),
                        resultSet.getInt("damage_dealt"),
                        resultSet.getInt("boss_remaining_hp"),
                        resultSet.getInt("xp_reward")
                ),
                raidAttemptId,
                userId
        );
        return attempts.stream()
                .findFirst()
                .orElseThrow(() -> new RaidAttemptNotFoundException(raidAttemptId));
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
                      AND status = 'CLEARED'
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

    private CharacterStats currentStats(long userId) {
        return jdbcTemplate.query(
                        """
                        SELECT total_xp, level, strength, vitality
                        FROM character_profiles
                        WHERE user_id = ?
                        """,
                        (resultSet, rowNumber) -> new CharacterStats(
                                resultSet.getLong("total_xp"),
                                resultSet.getInt("level"),
                                resultSet.getInt("strength"),
                                resultSet.getInt("vitality")
                        ),
                        userId
                )
                .stream()
                .findFirst()
                .orElse(new CharacterStats(0, 1, 1, 1));
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

    private record RaidAttemptState(
            long id,
            long bossRaidId,
            String bossName,
            int stage,
            String status,
            int damageDealt,
            int bossRemainingHp,
            int xpReward
    ) {
    }

    private record CharacterStats(long totalXp, int level, int strength, int vitality) {
    }
}
