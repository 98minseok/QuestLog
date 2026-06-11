package com.als98.questlog.be;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
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
class InitialSchemaMigrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsUserAndGoalWithDefaults() {
        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO app_users (external_subject, display_name)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                "keycloak-user-1",
                "Quest Hero"
        );

        Long goalId = jdbcTemplate.queryForObject(
                """
                INSERT INTO goals (user_id, title, target_date)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                userId,
                "Run a half marathon",
                Date.valueOf(LocalDate.of(2026, 10, 1))
        );

        Map<String, Object> goal = jdbcTemplate.queryForMap(
                "SELECT user_id, title, status FROM goals WHERE id = ?",
                goalId
        );

        assertThat(goal)
                .containsEntry("user_id", userId)
                .containsEntry("title", "Run a half marathon")
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void rejectsUnsupportedGoalStatus() {
        Long userId = jdbcTemplate.queryForObject(
                """
                INSERT INTO app_users (external_subject, display_name)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                "keycloak-user-2",
                "Quest Mage"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO goals (user_id, title, status)
                        VALUES (?, ?, ?)
                        """,
                        userId,
                        "Invalid goal",
                        "PAUSED"
                ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
