package com.als98.questlog.be.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final CurrentUserResolver currentUserResolver;
    private final JdbcTemplate jdbcTemplate;

    public CurrentUserService(CurrentUserResolver currentUserResolver, JdbcTemplate jdbcTemplate) {
        this.currentUserResolver = currentUserResolver;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long currentUserId() {
        CurrentUser currentUser = currentUserResolver.resolve();
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO app_users (external_subject, display_name, timezone)
                VALUES (?, ?, ?)
                ON CONFLICT (external_subject) DO UPDATE
                SET display_name = EXCLUDED.display_name,
                    timezone = EXCLUDED.timezone,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """,
                Long.class,
                currentUser.subject(),
                currentUser.displayName(),
                currentUser.timezone()
        );
    }
}
