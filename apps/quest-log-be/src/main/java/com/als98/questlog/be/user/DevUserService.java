package com.als98.questlog.be.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevUserService {

    public static final String EXTERNAL_SUBJECT = "dev-user";

    private final JdbcTemplate jdbcTemplate;

    public DevUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long currentUserId() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO app_users (external_subject, display_name, timezone)
                VALUES (?, 'Quest Hero', 'Asia/Seoul')
                ON CONFLICT (external_subject) DO UPDATE
                SET updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """,
                Long.class,
                EXTERNAL_SUBJECT
        );
    }
}
