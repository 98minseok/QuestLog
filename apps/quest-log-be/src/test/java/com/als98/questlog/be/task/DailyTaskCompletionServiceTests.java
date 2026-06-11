package com.als98.questlog.be.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.als98.questlog.be.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DailyTaskCompletionServiceTests {

    @Autowired
    private DailyTaskCompletionService completionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completesTaskAndCreatesCharacterProgression() {
        long userId = createUser("completion-user-1");
        long taskId = createTask(userId, 120);

        DailyTaskCompletionResult result = completionService.complete(userId, taskId);

        assertThat(result.taskId()).isEqualTo(taskId);
        assertThat(result.xpAwarded()).isEqualTo(120);
        assertThat(result.totalXp()).isEqualTo(120);
        assertThat(result.level()).isEqualTo(2);
        assertThat(result.strength()).isEqualTo(2);
        assertThat(result.vitality()).isEqualTo(2);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_tasks WHERE id = ?",
                String.class,
                taskId
        )).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForMap(
                """
                SELECT task_id, xp_awarded
                FROM task_completions
                WHERE id = ?
                """,
                result.completionId()
        ))
                .containsEntry("task_id", taskId)
                .containsEntry("xp_awarded", 120);
        assertThat(jdbcTemplate.queryForMap(
                """
                SELECT user_id, total_xp, level, strength, vitality
                FROM character_profiles
                WHERE user_id = ?
                """,
                userId
        ))
                .containsEntry("user_id", userId)
                .containsEntry("total_xp", 120L)
                .containsEntry("level", 2)
                .containsEntry("strength", 2)
                .containsEntry("vitality", 2);
    }

    @Test
    void rejectsDuplicateCompletionWithoutAwardingExperienceAgain() {
        long userId = createUser("completion-user-2");
        long taskId = createTask(userId, 40);
        completionService.complete(userId, taskId);

        assertThatThrownBy(() -> completionService.complete(userId, taskId))
                .isInstanceOf(DailyTaskAlreadyCompletedException.class)
                .hasMessageContaining("already been completed");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task_completions WHERE task_id = ?",
                Integer.class,
                taskId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_xp FROM character_profiles WHERE user_id = ?",
                Long.class,
                userId
        )).isEqualTo(40L);
    }

    @Test
    void rejectsCompletionByAnotherUserWithoutChangingEitherUser() {
        long ownerId = createUser("completion-user-3");
        long otherUserId = createUser("completion-user-4");
        long taskId = createTask(ownerId, 25);

        assertThatThrownBy(() -> completionService.complete(otherUserId, taskId))
                .isInstanceOf(DailyTaskNotFoundException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM daily_tasks WHERE id = ?",
                String.class,
                taskId
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task_completions WHERE task_id = ?",
                Integer.class,
                taskId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM character_profiles WHERE user_id IN (?, ?)",
                Integer.class,
                ownerId,
                otherUserId
        )).isZero();
    }

    private long createUser(String externalSubject) {
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

    private long createTask(long userId, int xpReward) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO daily_tasks (user_id, title, xp_reward)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                "Complete a focused quest",
                xpReward
        );
    }
}
