package com.als98.questlog.be;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ProgressionSchemaMigrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsTaskCharacterAndRaidAttemptWithDefaults() {
        Long userId = createUser("progression-user-1");
        Long goalId = createGoal(userId, "Build a reading habit");

        Long taskId = jdbcTemplate.queryForObject(
                """
                INSERT INTO daily_tasks (user_id, goal_id, title)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                goalId,
                "Read for 20 minutes"
        );
        jdbcTemplate.update(
                "INSERT INTO task_completions (task_id, xp_awarded) VALUES (?, ?)",
                taskId,
                10
        );
        Long weeklyQuestId = jdbcTemplate.queryForObject(
                """
                INSERT INTO weekly_quests (user_id, goal_id, title, week_start_date)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                goalId,
                "Publish a weekly progress note",
                java.sql.Date.valueOf("2026-06-15")
        );
        jdbcTemplate.update(
                "INSERT INTO weekly_quest_completions (weekly_quest_id, xp_awarded) VALUES (?, ?)",
                weeklyQuestId,
                75
        );
        jdbcTemplate.update(
                """
                INSERT INTO recommendation_history (
                    user_id, goal_id, provider, action, title, task_date, xp_reward
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                goalId,
                "deterministic-mock",
                "PREVIEWED",
                "Plan the next step",
                java.sql.Date.valueOf("2026-06-14"),
                10
        );
        jdbcTemplate.update(
                "INSERT INTO character_profiles (user_id) VALUES (?)",
                userId
        );
        jdbcTemplate.update(
                """
                INSERT INTO character_progression_events (
                    user_id, source_type, source_id, xp_awarded,
                    total_xp, level, strength, vitality
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                "DAILY_TASK",
                taskId,
                10,
                10,
                1,
                1,
                1
        );
        Long bossRaidId = jdbcTemplate.queryForObject(
                """
                INSERT INTO boss_raids (stage, name, max_hp, xp_reward)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                101,
                "Slime King",
                100,
                50
        );
        jdbcTemplate.update(
                "INSERT INTO raid_attempts (user_id, boss_raid_id) VALUES (?, ?)",
                userId,
                bossRaidId
        );

        assertThat(jdbcTemplate.queryForMap(
                "SELECT status, source, xp_reward FROM daily_tasks WHERE id = ?",
                taskId
        ))
                .containsEntry("status", "PENDING")
                .containsEntry("source", "MANUAL")
                .containsEntry("xp_reward", 10);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT status, source, xp_reward FROM weekly_quests WHERE id = ?",
                weeklyQuestId
        ))
                .containsEntry("status", "PENDING")
                .containsEntry("source", "SYSTEM")
                .containsEntry("xp_reward", 75);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT provider, action, source, xp_reward FROM recommendation_history WHERE goal_id = ?",
                goalId
        ))
                .containsEntry("provider", "deterministic-mock")
                .containsEntry("action", "PREVIEWED")
                .containsEntry("source", "AI_RECOMMENDED")
                .containsEntry("xp_reward", 10);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT level, total_xp, strength, vitality FROM character_profiles WHERE user_id = ?",
                userId
        ))
                .containsEntry("level", 1)
                .containsEntry("total_xp", 0L)
                .containsEntry("strength", 1)
                .containsEntry("vitality", 1);
        assertThat(jdbcTemplate.queryForMap(
                """
                SELECT source_type, source_id, xp_awarded, total_xp, level
                FROM character_progression_events
                WHERE user_id = ?
                """,
                userId
        ))
                .containsEntry("source_type", "DAILY_TASK")
                .containsEntry("source_id", taskId)
                .containsEntry("xp_awarded", 10)
                .containsEntry("total_xp", 10L)
                .containsEntry("level", 1);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT status, damage_dealt FROM raid_attempts WHERE user_id = ?",
                userId
        ))
                .containsEntry("status", "IN_PROGRESS")
                .containsEntry("damage_dealt", 0);
    }

    @Test
    void rejectsTaskLinkedToAnotherUsersGoal() {
        Long goalOwnerId = createUser("progression-user-2");
        Long taskOwnerId = createUser("progression-user-3");
        Long goalId = createGoal(goalOwnerId, "Learn PostgreSQL");

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO daily_tasks (user_id, goal_id, title)
                VALUES (?, ?, ?)
                """,
                taskOwnerId,
                goalId,
                "Read the documentation"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMoreThanOneCompletionForATask() {
        Long userId = createUser("progression-user-4");
        Long taskId = jdbcTemplate.queryForObject(
                """
                INSERT INTO daily_tasks (user_id, title)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                "Complete one quest"
        );
        jdbcTemplate.update(
                "INSERT INTO task_completions (task_id, xp_awarded) VALUES (?, ?)",
                taskId,
                10
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO task_completions (task_id, xp_awarded) VALUES (?, ?)",
                taskId,
                10
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMoreThanOneCompletionForAWeeklyQuest() {
        Long userId = createUser("progression-user-6");
        Long weeklyQuestId = jdbcTemplate.queryForObject(
                """
                INSERT INTO weekly_quests (user_id, title, week_start_date)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                "Complete a weekly review",
                java.sql.Date.valueOf("2026-06-15")
        );
        jdbcTemplate.update(
                "INSERT INTO weekly_quest_completions (weekly_quest_id, xp_awarded) VALUES (?, ?)",
                weeklyQuestId,
                75
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO weekly_quest_completions (weekly_quest_id, xp_awarded) VALUES (?, ?)",
                weeklyQuestId,
                75
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsFinishedRaidWithoutCompletionTimestamp() {
        Long userId = createUser("progression-user-5");
        Long bossRaidId = jdbcTemplate.queryForObject(
                """
                INSERT INTO boss_raids (stage, name, max_hp, xp_reward)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                102,
                "Stone Guardian",
                250,
                100
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO raid_attempts (user_id, boss_raid_id, status)
                VALUES (?, ?, ?)
                """,
                userId,
                bossRaidId,
                "VICTORY"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long createUser(String externalSubject) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO app_users (external_subject, display_name)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                externalSubject,
                "Quest Hero"
        );
    }

    private Long createGoal(Long userId, String title) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO goals (user_id, title)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                title
        );
    }
}
